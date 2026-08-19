package com.sonnesen;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        assertThrows(DuplicateEmailException.class, () -> repository.insert(newCustomer("dup@example.com")));
    }
}
