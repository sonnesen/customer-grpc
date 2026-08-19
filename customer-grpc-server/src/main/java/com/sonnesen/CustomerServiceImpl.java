package com.sonnesen;

import com.sonnesen.customer.grpc.CreateCustomerRequest;
import com.sonnesen.customer.grpc.Customer;
import com.sonnesen.customer.grpc.CustomerServiceGrpc;
import com.sonnesen.customer.grpc.GetCustomerRequest;
import com.sonnesen.customer.grpc.ListCustomersRequest;
import com.sonnesen.customer.grpc.ListCustomersResponse;

import io.grpc.stub.StreamObserver;

public class CustomerServiceImpl extends CustomerServiceGrpc.CustomerServiceImplBase {

    private final CustomerRepository repository;

    public CustomerServiceImpl(CustomerRepository repository) {
        this.repository = repository;
    }

    @Override
    public void createCustomer(CreateCustomerRequest request, StreamObserver<Customer> responseObserver) {
        if (request.getName().isBlank() || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("name and email are required");
        }

        // DuplicateEmailException, if thrown, propagates to ExceptionHandlingInterceptor.
        com.sonnesen.Customer created = repository.insert(CustomerMapper.toDomain(request));
        responseObserver.onNext(CustomerMapper.toProto(created));
        responseObserver.onCompleted();
    }

    @Override
    public void getCustomer(GetCustomerRequest request, StreamObserver<Customer> responseObserver) {
        com.sonnesen.Customer customer = repository.findById(request.getId())
            .orElseThrow(() -> new CustomerNotFoundException(request.getId()));

        responseObserver.onNext(CustomerMapper.toProto(customer));
        responseObserver.onCompleted();
    }

    @Override
    public void listCustomers(ListCustomersRequest request, StreamObserver<ListCustomersResponse> responseObserver) {
        ListCustomersResponse response = ListCustomersResponse.newBuilder()
            .addAllCustomers(repository.findAll().stream().map(CustomerMapper::toProto).toList())
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
