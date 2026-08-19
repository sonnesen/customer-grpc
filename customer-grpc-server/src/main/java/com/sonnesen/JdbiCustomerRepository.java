package com.sonnesen;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Jdbi;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

public class JdbiCustomerRepository implements CustomerRepository {

    private final Jdbi jdbi;

    public JdbiCustomerRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public Customer insert(Customer customer) {
        try {
            return jdbi.withHandle(handle -> {
                long id = handle.createUpdate("""
                        INSERT INTO customers (name, email, phone, street, city, state, zip_code, status)
                        VALUES (:name, :email, :phone, :street, :city, :state, :zipCode, :status)
                        """)
                    .bind("name", customer.name())
                    .bind("email", customer.email())
                    .bind("phone", customer.phone())
                    .bind("street", customer.address().street())
                    .bind("city", customer.address().city())
                    .bind("state", customer.address().state())
                    .bind("zipCode", customer.address().zipCode())
                    .bind("status", customer.status().name())
                    .executeAndReturnGeneratedKeys("id")
                    .mapTo(Long.class)
                    .one();

                return customer.withId(id);
            });
        } catch (RuntimeException e) {
            if (isUniqueViolation(e)) {
                throw new DuplicateEmailException(customer.email());
            }
            throw e;
        }
    }

    @Override
    public Optional<Customer> findById(long id) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT * FROM customers WHERE id = :id")
            .bind("id", id)
            .map(new CustomerRowMapper())
            .findOne());
    }

    @Override
    public List<Customer> findAll() {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT * FROM customers ORDER BY id")
            .map(new CustomerRowMapper())
            .list());
    }

    private static boolean isUniqueViolation(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof PSQLException psqlException
                && PSQLState.UNIQUE_VIOLATION.getState().equals(psqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
