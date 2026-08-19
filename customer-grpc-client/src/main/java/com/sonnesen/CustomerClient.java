package com.sonnesen;

import java.util.concurrent.TimeUnit;

import com.sonnesen.customer.grpc.Address;
import com.sonnesen.customer.grpc.CreateCustomerRequest;
import com.sonnesen.customer.grpc.Customer;
import com.sonnesen.customer.grpc.CustomerServiceGrpc;
import com.sonnesen.customer.grpc.CustomerServiceGrpc.CustomerServiceBlockingStub;
import com.sonnesen.customer.grpc.GetCustomerRequest;
import com.sonnesen.customer.grpc.ListCustomersRequest;
import com.sonnesen.customer.grpc.ListCustomersResponse;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;

/**
 * Thin wrapper around the generated {@link CustomerServiceBlockingStub}, used
 * both as a small SDK and as the demo entry point in {@link Main}.
 */
public class CustomerClient implements AutoCloseable {

    private final ManagedChannel channel;
    private final CustomerServiceBlockingStub blockingStub;

    public CustomerClient(String host, int port) {
        this.channel = Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create())
            .build();
        this.blockingStub = CustomerServiceGrpc.newBlockingStub(channel);
    }

    public Customer createCustomer(String name, String email, String phone, Address address) {
        CreateCustomerRequest request = CreateCustomerRequest.newBuilder()
            .setName(name)
            .setEmail(email)
            .setPhone(phone)
            .setAddress(address)
            .build();
        return blockingStub.createCustomer(request);
    }

    public Customer getCustomer(long id) {
        return blockingStub.getCustomer(GetCustomerRequest.newBuilder().setId(id).build());
    }

    public ListCustomersResponse listCustomers() {
        return blockingStub.listCustomers(ListCustomersRequest.newBuilder().build());
    }

    @Override
    public void close() {
        channel.shutdown();
        try {
            channel.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
