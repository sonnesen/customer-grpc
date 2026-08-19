package com.sonnesen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.sonnesen.customer.grpc.Address;
import com.sonnesen.customer.grpc.CreateCustomerRequest;
import com.sonnesen.customer.grpc.Customer;
import com.sonnesen.customer.grpc.CustomerServiceGrpc;
import com.sonnesen.customer.grpc.CustomerServiceGrpc.CustomerServiceBlockingStub;
import com.sonnesen.customer.grpc.DeleteCustomerRequest;
import com.sonnesen.customer.grpc.GetCustomerRequest;
import com.sonnesen.customer.grpc.ListCustomersRequest;
import com.sonnesen.customer.grpc.ListCustomersResponse;
import com.sonnesen.customer.grpc.UpdateCustomerRequest;

import io.grpc.Server;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.ManagedChannel;

/**
 * Exercises {@link CustomerServiceImpl} end-to-end (RPC in, RPC out) over an
 * in-process gRPC channel backed by a real PostgreSQL instance, so the
 * status codes coming out of {@link ExceptionHandlingInterceptor} are
 * verified the same way a real client would see them.
 */
@Testcontainers
class CustomerServiceImplTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    private static Server server;
    private static ManagedChannel channel;
    private static CustomerServiceBlockingStub client;

    @BeforeAll
    static void startServer() throws IOException {
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .load()
            .migrate();

        Jdbi jdbi = Jdbi.create(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .installPlugin(new PostgresPlugin());

        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new CustomerServiceImpl(new JdbiCustomerRepository(jdbi)))
            .intercept(new ExceptionHandlingInterceptor())
            .build()
            .start();

        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        client = CustomerServiceGrpc.newBlockingStub(channel);
    }

    @AfterAll
    static void stopServer() throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    private static final Address SOME_ADDRESS = Address.newBuilder()
        .setStreet("Rua das Flores, 123")
        .setCity("Porto Alegre")
        .setState("RS")
        .setZipCode("90000-000")
        .build();

    @BeforeEach
    void cleanTable() {
        Jdbi.create(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .useHandle(handle -> handle.execute("DELETE FROM customers"));
    }

    private static CreateCustomerRequest createRequest(String email) {
        return CreateCustomerRequest.newBuilder()
            .setName("Ada Lovelace")
            .setEmail(email)
            .setPhone("+55 51 90000-0000")
            .setAddress(SOME_ADDRESS)
            .build();
    }

    @Test
    void createThenGetReturnsTheSameCustomer() {
        Customer created = client.createCustomer(createRequest("ada@example.com"));

        Customer fetched = client.getCustomer(GetCustomerRequest.newBuilder().setId(created.getId()).build());

        assertEquals(created, fetched);
    }

    @Test
    void createWithBlankNameFailsWithInvalidArgument() {
        CreateCustomerRequest request = createRequest("blank-name@example.com").toBuilder()
            .setName("")
            .build();

        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> client.createCustomer(request));
        assertEquals(io.grpc.Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
    }

    @Test
    void createWithDuplicateEmailFailsWithAlreadyExists() {
        client.createCustomer(createRequest("dup@example.com"));
        CreateCustomerRequest secondWithSameEmail = createRequest("dup@example.com");

        StatusRuntimeException e = assertThrows(StatusRuntimeException.class,
            () -> client.createCustomer(secondWithSameEmail));
        assertEquals(io.grpc.Status.Code.ALREADY_EXISTS, e.getStatus().getCode());
    }

    @Test
    void getUnknownCustomerFailsWithNotFound() {
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class,
            () -> client.getCustomer(GetCustomerRequest.newBuilder().setId(-1).build()));
        assertEquals(io.grpc.Status.Code.NOT_FOUND, e.getStatus().getCode());
    }

    @Test
    void listCustomersReturnsAllCreatedCustomers() {
        client.createCustomer(createRequest("a@example.com"));
        client.createCustomer(createRequest("b@example.com"));

        var response = client.listCustomers(ListCustomersRequest.newBuilder().build());

        assertEquals(2, response.getCustomersCount());
    }

    @Test
    void listCustomersPaginatesWithPageSizeAndToken() {
        client.createCustomer(createRequest("a@example.com"));
        client.createCustomer(createRequest("b@example.com"));
        client.createCustomer(createRequest("c@example.com"));

        ListCustomersResponse firstPage = client.listCustomers(
            ListCustomersRequest.newBuilder().setPageSize(2).build());
        assertEquals(2, firstPage.getCustomersCount());
        assertFalse(firstPage.getNextPageToken().isEmpty());

        ListCustomersResponse secondPage = client.listCustomers(
            ListCustomersRequest.newBuilder().setPageSize(2).setPageToken(firstPage.getNextPageToken()).build());
        assertEquals(1, secondPage.getCustomersCount());
        assertTrue(secondPage.getNextPageToken().isEmpty());
    }

    @Test
    void updateCustomerChangesFieldsAndKeepsId() {
        Customer created = client.createCustomer(createRequest("before@example.com"));
        UpdateCustomerRequest request = UpdateCustomerRequest.newBuilder()
            .setId(created.getId())
            .setName("Ada Byron")
            .setEmail("after@example.com")
            .setPhone("+55 51 90000-9999")
            .setAddress(SOME_ADDRESS)
            .build();

        Customer updated = client.updateCustomer(request);

        assertEquals(created.getId(), updated.getId());
        assertEquals("Ada Byron", updated.getName());
        assertEquals("after@example.com", updated.getEmail());
    }

    @Test
    void updateUnknownCustomerFailsWithNotFound() {
        UpdateCustomerRequest request = UpdateCustomerRequest.newBuilder()
            .setId(-1)
            .setName("Ada Byron")
            .setEmail("ghost@example.com")
            .setPhone("+55 51 90000-9999")
            .setAddress(SOME_ADDRESS)
            .build();

        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> client.updateCustomer(request));
        assertEquals(io.grpc.Status.Code.NOT_FOUND, e.getStatus().getCode());
    }

    @Test
    void deleteCustomerThenGetFailsWithNotFound() {
        Customer created = client.createCustomer(createRequest("todelete@example.com"));

        client.deleteCustomer(DeleteCustomerRequest.newBuilder().setId(created.getId()).build());

        GetCustomerRequest getRequest = GetCustomerRequest.newBuilder().setId(created.getId()).build();
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> client.getCustomer(getRequest));
        assertEquals(io.grpc.Status.Code.NOT_FOUND, e.getStatus().getCode());
    }

    @Test
    void deleteCustomerIsIdempotentForUnknownId() {
        // Deleting an id that doesn't exist should not fail the RPC.
        client.deleteCustomer(DeleteCustomerRequest.newBuilder().setId(-1).build());
    }
}
