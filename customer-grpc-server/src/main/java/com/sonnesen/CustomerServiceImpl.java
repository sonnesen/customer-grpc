package com.sonnesen;

import com.sonnesen.customer.grpc.CreateCustomerRequest;
import com.sonnesen.customer.grpc.Customer;
import com.sonnesen.customer.grpc.CustomerServiceGrpc;
import com.sonnesen.customer.grpc.GetCustomerRequest;
import com.sonnesen.customer.grpc.ListCustomersRequest;
import com.sonnesen.customer.grpc.ListCustomersResponse;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class CustomerServiceImpl extends CustomerServiceGrpc.CustomerServiceImplBase {

    private final CustomerRepository repository;

    public CustomerServiceImpl(CustomerRepository repository) {
        this.repository = repository;
    }

    @Override
    public void createCustomer(CreateCustomerRequest request, StreamObserver<Customer> responseObserver) {
        if (request.getName().isBlank() || request.getEmail().isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription("name and email are required")
                .asRuntimeException());
            return;
        }

        try {
            com.sonnesen.Customer created = repository.insert(CustomerMapper.toDomain(request));
            responseObserver.onNext(CustomerMapper.toProto(created));
            responseObserver.onCompleted();
        } catch (DuplicateEmailException e) {
            responseObserver.onError(Status.ALREADY_EXISTS
                .withDescription(e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void getCustomer(GetCustomerRequest request, StreamObserver<Customer> responseObserver) {
        repository.findById(request.getId())
            .ifPresentOrElse(
                customer -> {
                    responseObserver.onNext(CustomerMapper.toProto(customer));
                    responseObserver.onCompleted();
                },
                () -> responseObserver.onError(Status.NOT_FOUND
                    .withDescription(new CustomerNotFoundException(request.getId()).getMessage())
                    .asRuntimeException())
            );
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
