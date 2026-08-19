package com.sonnesen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class JdbiCustomerRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static Jdbi jdbi;

    private CustomerRepository repository;

    @BeforeAll
    static void migrate() {
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .load()
            .migrate();

        jdbi = Jdbi.create(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .installPlugin(new PostgresPlugin());
    }

    @BeforeEach
    void cleanTable() {
        jdbi.useHandle(handle -> handle.execute("DELETE FROM customers"));
        repository = new JdbiCustomerRepository(jdbi);
    }

    private static Customer newCustomer(String email) {
        return new Customer(
            null,
            "Ada Lovelace",
            email,
            "+55 51 90000-0000",
            new Address("Rua das Flores, 123", "Porto Alegre", "RS", "90000-000"),
            CustomerStatus.ACTIVE
        );
    }

    @Test
    void insertAssignsGeneratedId() {
        Customer inserted = repository.insert(newCustomer("ada@example.com"));

        assertTrue(inserted.id() > 0);
        assertEquals("ada@example.com", inserted.email());
    }

    @Test
    void findByIdReturnsPersistedCustomer() {
        Customer inserted = repository.insert(newCustomer("grace@example.com"));

        Optional<Customer> found = repository.findById(inserted.id());

        assertTrue(found.isPresent());
        assertEquals(inserted, found.get());
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        assertTrue(repository.findById(-1).isEmpty());
    }

    @Test
    void findAllReturnsEveryPersistedCustomer() {
        repository.insert(newCustomer("a@example.com"));
        repository.insert(newCustomer("b@example.com"));

        List<Customer> all = repository.findAll();

        assertEquals(2, all.size());
    }

    @Test
    void insertWithDuplicateEmailThrowsDuplicateEmailException() {
        repository.insert(newCustomer("dup@example.com"));
        Customer secondWithSameEmail = newCustomer("dup@example.com");

        assertThrows(DuplicateEmailException.class, () -> repository.insert(secondWithSameEmail));
    }

    @Test
    void updateChangesFieldsButKeepsStatus() {
        Customer inserted = repository.insert(newCustomer("before@example.com"));
        Address newAddress = new Address("Av. Ipiranga, 1", "Porto Alegre", "RS", "90000-999");

        Optional<Customer> updated = repository.update(
            inserted.id(), "Ada Byron", "after@example.com", "+55 51 90000-9999", newAddress);

        assertTrue(updated.isPresent());
        assertEquals("Ada Byron", updated.get().name());
        assertEquals("after@example.com", updated.get().email());
        assertEquals(newAddress, updated.get().address());
        assertEquals(CustomerStatus.ACTIVE, updated.get().status());
    }

    @Test
    void updateReturnsEmptyWhenMissing() {
        Optional<Customer> updated = repository.update(
            -1, "Ada Byron", "ghost@example.com", "+55 51 90000-9999",
            new Address("Av. Ipiranga, 1", "Porto Alegre", "RS", "90000-999"));

        assertTrue(updated.isEmpty());
    }

    @Test
    void updateWithEmailAlreadyUsedByAnotherCustomerThrowsDuplicateEmailException() {
        repository.insert(newCustomer("taken@example.com"));
        Customer other = repository.insert(newCustomer("other@example.com"));
        Address address = other.address();

        assertThrows(DuplicateEmailException.class,
            () -> repository.update(other.id(), other.name(), "taken@example.com", other.phone(), address));
    }

    @Test
    void deleteByIdRemovesCustomerAndReturnsTrue() {
        Customer inserted = repository.insert(newCustomer("todelete@example.com"));

        assertTrue(repository.deleteById(inserted.id()));
        assertTrue(repository.findById(inserted.id()).isEmpty());
    }

    @Test
    void deleteByIdReturnsFalseWhenMissing() {
        assertFalse(repository.deleteById(-1));
    }

    @Test
    void findPageReturnsCustomersAfterCursorInIdOrder() {
        Customer a = repository.insert(newCustomer("a@example.com"));
        Customer b = repository.insert(newCustomer("b@example.com"));
        Customer c = repository.insert(newCustomer("c@example.com"));

        List<Customer> firstPage = repository.findPage(0, 2);
        assertEquals(List.of(a, b), firstPage);

        List<Customer> secondPage = repository.findPage(b.id(), 2);
        assertEquals(List.of(c), secondPage);
    }
}
