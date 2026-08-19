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

public class CustomerServer {

    private static final Logger logger = LogManager.getLogger(CustomerServer.class.getName());

    private Server server;

    private void start() throws IOException {
        int port = Integer.parseInt(System.getProperty("server.port", "50051"));
        int nThreads = Integer.parseInt(System.getProperty("server.threads", "2"));

        logger.info("Running database migrations and connecting to PostgreSQL");
        Jdbi jdbi = Database.bootstrap();
        CustomerRepository repository = new JdbiCustomerRepository(jdbi);

        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
            .executor(executor)
            .addService(new CustomerServiceImpl(repository))
            .build()
            .start();

        logger.info("Server started, listening on {}", port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.error("*** shutting down gRPC server since JVM is shutting down");
                try {
                    CustomerServer.this.stop();
                } catch (InterruptedException e) {
                    if (server != null) {
                        server.shutdownNow();
                    }
                    logger.error("Server shutdown interrupted", e);
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