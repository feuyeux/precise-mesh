package org.feuyeux.given.proto.server;

import java.io.IOException;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProtoServer {
    private final Server server;

    public ProtoServer(final int port) throws IOException {
        this.server = ServerBuilder.forPort(port).addService(new LandingServiceImpl()).build();
        start();
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        ProtoServer server = new ProtoServer(50061);
        server.blockUntilShutdown();
    }

    private void start() throws IOException {
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.warn("shutting down Google RPC Server since JVM is shutting down");
            ProtoServer.this.stop();
            log.warn("Google RPC Server shut down");
        }));
        log.info("Google RPC Server is launched.");
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public void stop() {
        server.shutdown();
    }
}
