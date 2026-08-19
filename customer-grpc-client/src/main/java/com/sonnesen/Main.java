package com.sonnesen;

import com.sonnesen.customer.grpc.Address;
import com.sonnesen.customer.grpc.Customer;
import com.sonnesen.customer.grpc.ListCustomersResponse;

/**
 * Small end-to-end demo: creates a customer, fetches it back by id, then
 * lists all customers known to the server. Useful to sanity-check the
 * server + PostgreSQL wiring without a separate gRPC client tool.
 */
public class Main {

    public static void main(String[] args) {
        String host = System.getProperty("server.host", "localhost");
        int port = Integer.parseInt(System.getProperty("server.port", "50052"));

        try (CustomerClient client = new CustomerClient(host, port)) {
            Address address = Address.newBuilder()
                .setStreet("Rua das Flores, 123")
                .setCity("Porto Alegre")
                .setState("RS")
                .setZipCode("90000-000")
                .build();

            Customer created = client.createCustomer(
                "Ada Lovelace", "ada@example.com", "+55 51 90000-0000", address);
            System.out.println("Created: " + created);

            Customer fetched = client.getCustomer(created.getId());
            System.out.println("Fetched: " + fetched);

            ListCustomersResponse listed = client.listCustomers();
            System.out.println("Total customers: " + listed.getCustomersCount());
            listed.getCustomersList().forEach(System.out::println);
        }
    }
}
