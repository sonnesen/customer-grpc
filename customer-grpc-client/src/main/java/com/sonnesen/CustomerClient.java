package com.sonnesen;

import java.util.concurrent.TimeUnit;

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

    public Customer updateCustomer(long id, String name, String email, String phone, Address address) {
        UpdateCustomerRequest request = UpdateCustomerRequest.newBuilder()
            .setId(id)
            .setName(name)
            .setEmail(email)
            .setPhone(phone)
            .setAddress(address)
            .build();
        return blockingStub.updateCustomer(request);
    }

    public void deleteCustomer(long id) {
        blockingStub.deleteCustomer(DeleteCustomerRequest.newBuilder().setId(id).build());
    }

    public ListCustomersResponse listCustomers() {
        return listCustomers(0, "");
    }

    public ListCustomersResponse listCustomers(int pageSize, String pageToken) {
        return blockingStub.listCustomers(ListCustomersRequest.newBuilder()
            .setPageSize(pageSize)
            .setPageToken(pageToken)
            .build());
    }

    @Override
    public void close() {
        channel.shutdown();
        try {
            channel.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
