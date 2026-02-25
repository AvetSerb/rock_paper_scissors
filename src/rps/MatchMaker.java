package rps;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MatchMaker {
    private final BlockingQueue<Player> lobby = new LinkedBlockingQueue<>();
    private final ExecutorService sessionPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setName("session-" + t.getId());
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean running = new AtomicBoolean(true);

    public MatchMaker() {
        sessionPool.execute(this::loop);
    }

    public void enqueue(Player player) {
        if (!running.get()) {
            player.send("Server is shutting down.");
            player.closeQuietly();
            return;
        }
        lobby.offer(player);
    }

    private void loop() {
        while (running.get()) {
            try {
                Player a = lobby.take();
                Player b = lobby.take();

                if (a.isClosed()) { requeueOrDrop(b); continue; }
                if (b.isClosed()) { requeueOrDrop(a); continue; }

                sessionPool.execute(new GameSession(a, b));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        drainAndClose();
    }

    private void requeueOrDrop(Player p) {
        if (p == null) return;
        if (p.isClosed()) return;
        lobby.offer(p);
    }

    public void shutdown() {
        running.set(false);
        sessionPool.shutdownNow();
    }

    private void drainAndClose() {
        Player p;
        while ((p = lobby.poll()) != null) {
            p.send("Server is shutting down.");
            p.closeQuietly();
        }
    }
}
