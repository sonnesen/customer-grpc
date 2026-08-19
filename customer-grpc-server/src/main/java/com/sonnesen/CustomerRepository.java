package com.sonnesen;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository {

    Customer insert(Customer customer);

    Optional<Customer> findById(long id);

    List<Customer> findAll();

    /**
     * Updates name/email/phone/address for the given customer id, leaving its
     * status untouched. Empty if no customer with that id exists.
     */
    Optional<Customer> update(long id, String name, String email, String phone, Address address);

    /**
     * @return true if a customer with that id existed (and was deleted), false
     * if there was nothing to delete.
     */
    boolean deleteById(long id);

    /**
     * Up to {@code limit} customers with id strictly greater than {@code afterId},
     * ordered by id. Pass {@code afterId} 0 to start from the beginning.
     */
    List<Customer> findPage(long afterId, int limit);
}
