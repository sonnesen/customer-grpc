package com.sonnesen;

import com.sonnesen.customer.grpc.Address;
import com.sonnesen.customer.grpc.Customer;
import com.sonnesen.customer.grpc.ListCustomersResponse;

import io.grpc.StatusRuntimeException;

/**
 * Small end-to-end demo exercising the full CustomerService: create, fetch,
 * update, paginate through the list, then delete and confirm it's gone.
 * Useful to sanity-check the server + PostgreSQL wiring without a separate
 * gRPC client tool.
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

            Customer updated = client.updateCustomer(
                created.getId(), "Ada Lovelace", "ada.lovelace@example.com", "+55 51 90000-0001", address);
            System.out.println("Updated: " + updated);

            System.out.println("Listing customers, one page at a time (page_size=1):");
            String pageToken = "";
            do {
                ListCustomersResponse page = client.listCustomers(1, pageToken);
                page.getCustomersList().forEach(c -> System.out.println("  - " + c.getName() + " (id=" + c.getId() + ")"));
                pageToken = page.getNextPageToken();
            } while (!pageToken.isEmpty());

            client.deleteCustomer(updated.getId());
            System.out.println("Deleted customer " + updated.getId());

            try {
                client.getCustomer(updated.getId());
                System.out.println("Unexpected: customer still found after delete");
            } catch (StatusRuntimeException e) {
                System.out.println("Confirmed deleted, getCustomer now fails with: " + e.getStatus().getCode());
            }
        }
    }
}
