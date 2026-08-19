package com.sonnesen;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository {

    Customer insert(Customer customer);

    Optional<Customer> findById(long id);

    List<Customer> findAll();
}
