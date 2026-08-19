package com.sonnesen;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jdbi.v3.core.Jdbi;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;

public class CustomerServer {

    private static final Logger logger = LogManager.getLogger(CustomerServer.class.getName());

    private Server server;
    private HealthStatusManager health;

    private void start() throws IOException {
        int port = Integer.parseInt(System.getProperty("server.port", "50052"));
        int nThreads = Integer.parseInt(System.getProperty("server.threads", "2"));

        logger.info("Running database migrations and connecting to PostgreSQL");
        Jdbi jdbi = Database.bootstrap();
        CustomerRepository repository = new JdbiCustomerRepository(jdbi);

        health = new HealthStatusManager();

        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
            .executor(executor)
            .addService(new CustomerServiceImpl(repository))
            .intercept(new ExceptionHandlingInterceptor())
            // lets grpcurl/grpcui discover and call CustomerService without the .proto file
            .addService(ProtoReflectionServiceV1.newInstance())
            // standard grpc.health.v1.Health service, e.g. for k8s liveness/readiness probes
            .addService(health.getHealthService())
            .build()
            .start();

        health.setStatus("", ServingStatus.SERVING);
        logger.info("Server started, listening on {}", port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.error("*** shutting down gRPC server since JVM is shutting down");
                if (health != null) {
                    health.enterTerminalState();
                }
                try {
                    CustomerServer.this.stop();
                } catch (InterruptedException e) {
                    if (server != null) {
                        server.shutdownNow();
                    }
                    logger.error("Server shutdown interrupted", e);
                    Thread.currentThread().interrupt();
                } finally {
                    executor.shutdown();
                }
                logger.error("*** server shut down");
            }
        });
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        final CustomerServer server = new CustomerServer();
        server.start();
        server.blockUntilShutdown();
    }
}