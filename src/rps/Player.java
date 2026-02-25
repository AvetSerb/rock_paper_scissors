package rps;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Player {
    private final String nick;
    private final BufferedReader in;
    private final PrintWriter out;
    private final Socket socket;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public Player(String nick, BufferedReader in, PrintWriter out, Socket socket) {
        this.nick = Objects.requireNonNull(nick);
        this.in = Objects.requireNonNull(in);
        this.out = Objects.requireNonNull(out);
        this.socket = Objects.requireNonNull(socket);
    }

    public String nick() { return nick; }

    public void send(String msg) {
        if (closed.get()) return;
        out.println(msg);
    }

    public boolean isClosed() {
        return closed.get() || socket.isClosed();
    }

    public void closeQuietly() {
        if (!closed.compareAndSet(false, true)) return;
        try { socket.close(); } catch (Exception ignored) {}
    }

    public CompletableFuture<Step> readMoveAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                while (!isClosed()) {
                    String line = in.readLine();
                    if (line == null) {
                        closeQuietly();
                        return null;
                    }
                    Step move = Step.parse(line);
                    if (move != null) return move;
                    send("Invalid input. Type rock/paper/scissors (камень/бумага/ножницы). Try again:");
                }
                return null;
            } catch (Exception e) {
                closeQuietly();
                return null;
            }
        });
    }
}
