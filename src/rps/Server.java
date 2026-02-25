package rps;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Server {
    private final int port;
    private final MatchMaker matchMaker = new MatchMaker();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private ServerSocket serverSocket;
    private ExecutorService acceptorPool;
    private ExecutorService clientPool;

    public Server(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) return;

        this.serverSocket = new ServerSocket(port);
        this.serverSocket.setReuseAddress(true);

        // Accept loop on a single thread; client handlers on a pool.
        this.acceptorPool = Executors.newSingleThreadExecutor(named("acceptor"));

        // Use virtual threads if available (Java 21+), else fallback to a cached pool.
        this.clientPool = Threading.clientExecutor(named("client"));

        // start acceptor
        acceptorPool.execute(this::acceptLoop);

        // block main thread
        blockUntilStopped();
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                socket.setSoTimeout((int) Duration.ofHours(2).toMillis()); // defensive
                clientPool.execute(new ConnectionHandler(socket, matchMaker));
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("Accept error: " + e.getMessage());
                }
            }
        }
    }

    private void blockUntilStopped() {
        // Busy-wait with sleep to avoid extra primitives; shutdown hook triggers stop()
        while (running.get()) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) return;

        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}

        matchMaker.shutdown();

        if (acceptorPool != null) acceptorPool.shutdownNow();
        if (clientPool != null) clientPool.shutdownNow();

        System.out.println("Server stopped.");
    }

    private static ThreadFactory named(String prefix) {
        Objects.requireNonNull(prefix);
        return r -> {
            Thread t = new Thread(r);
            t.setName(prefix + "-" + t.getId());
            t.setDaemon(true);
            return t;
        };
    }
}
