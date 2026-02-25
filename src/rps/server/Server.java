package rps.server;

import rps.game.ConnectionHandler;
import rps.game.MatchMaker;

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
        this.acceptorPool = Executors.newSingleThreadExecutor(named("acceptor"));
        this.clientPool = Threading.clientExecutor(named("client"));
        acceptorPool.execute(this::acceptLoop);
        blockUntilStopped();
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                socket.setSoTimeout((int) Duration.ofHours(2).toMillis());
                clientPool.execute(new ConnectionHandler(socket, matchMaker));
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("Accept error: " + e.getMessage());
                }
            }
        }
    }

    private void blockUntilStopped() {
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
        } catch (IOException ignored) {
        }

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
