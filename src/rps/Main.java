package rps;

import java.io.IOException;

public final class Main {
    public static void main(String[] args) throws IOException {
        int port = Arguments.parsePort(args, 2323);
        Server server = new Server(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "shutdown-hook"));

        System.out.println("RPS server listening on 0.0.0.0:" + port);
        server.start(); // blocking
    }
}