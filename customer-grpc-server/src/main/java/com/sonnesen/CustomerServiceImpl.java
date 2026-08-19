package com.sonnesen;

import com.google.protobuf.Empty;

import com.sonnesen.customer.grpc.CreateCustomerRequest;
import com.sonnesen.customer.grpc.Customer;
import com.sonnesen.customer.grpc.CustomerServiceGrpc;
import com.sonnesen.customer.grpc.DeleteCustomerRequest;
import com.sonnesen.customer.grpc.GetCustomerRequest;
import com.sonnesen.customer.grpc.ListCustomersRequest;
import com.sonnesen.customer.grpc.ListCustomersResponse;
import com.sonnesen.customer.grpc.UpdateCustomerRequest;

import io.grpc.stub.StreamObserver;

public class CustomerServiceImpl extends CustomerServiceGrpc.CustomerServiceImplBase {

    // Default/max page size for ListCustomers when the client leaves page_size unset or asks for too much.
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

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
    public void updateCustomer(UpdateCustomerRequest request, StreamObserver<Customer> responseObserver) {
        if (request.getName().isBlank() || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("name and email are required");
        }

        com.sonnesen.Customer updated = repository.update(
                request.getId(),
                request.getName(),
                request.getEmail(),
                request.getPhone(),
                CustomerMapper.toDomain(request.getAddress()))
            .orElseThrow(() -> new CustomerNotFoundException(request.getId()));

        responseObserver.onNext(CustomerMapper.toProto(updated));
        responseObserver.onCompleted();
    }

    @Override
    public void deleteCustomer(DeleteCustomerRequest request, StreamObserver<Empty> responseObserver) {
        // Deleting an id that's already gone is not an error: repeating the same
        // delete request has the same end state, which is what "idempotent" means here.
        repository.deleteById(request.getId());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void listCustomers(ListCustomersRequest request, StreamObserver<ListCustomersResponse> responseObserver) {
        int pageSize = request.getPageSize() <= 0 ? DEFAULT_PAGE_SIZE : Math.min(request.getPageSize(), MAX_PAGE_SIZE);
        long afterId = PageToken.decode(request.getPageToken());

        // Fetch one extra row to find out, without a separate count query, whether another page follows.
        var page = repository.findPage(afterId, pageSize + 1);
        boolean hasNextPage = page.size() > pageSize;
        var customersOnPage = hasNextPage ? page.subList(0, pageSize) : page;

        ListCustomersResponse.Builder response = ListCustomersResponse.newBuilder()
            .addAllCustomers(customersOnPage.stream().map(CustomerMapper::toProto).toList());

        if (hasNextPage) {
            response.setNextPageToken(PageToken.encode(customersOnPage.get(customersOnPage.size() - 1).id()));
        }

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

}
