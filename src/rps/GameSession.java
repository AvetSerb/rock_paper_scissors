package rps;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class GameSession implements Runnable{
    private final Player p1;
    private final Player p2;

    private static final long MOVE_TIMEOUT_SECONDS = 120;

    public GameSession(Player p1, Player p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    @Override
    public void run() {
        try {
            p1.send("Opponent found: " + p2.nick());
            p2.send("Opponent found: " + p1.nick());

            p1.send("Type your move: rock/paper/scissors (камень/бумага/ножницы)");
            p2.send("Type your move: rock/paper/scissors (камень/бумага/ножницы)");

            while (!p1.isClosed() && !p2.isClosed()) {
                RoundResult res = playRound();
                if (res == null) return; // someone disconnected / timeout
                if (res.draw()) {
                    p1.send("Draw (" + res.p1Step() + " vs " + res.p2Step() + "). Repeat round!");
                    p2.send("Draw (" + res.p2Step() + " vs " + res.p1Step() + "). Repeat round!");
                    continue;
                }

                if (res.p1Wins()) {
                    p1.send("You WIN! (" + res.p1Step() + " beats " + res.p2Step() + ")");
                    p2.send("You LOSE! (" + res.p2Step() + " loses to " + res.p1Step() + ")");
                } else {
                    p2.send("You WIN! (" + res.p2Step() + " beats " + res.p1Step() + ")");
                    p1.send("You LOSE! (" + res.p1Step() + " loses to " + res.p2Step() + ")");
                }
                break;
            }
        } finally {
            p1.closeQuietly();
            p2.closeQuietly();
        }
    }

    private RoundResult playRound() {
        CompletableFuture<Step> f1 = p1.readMoveAsync();
        CompletableFuture<Step> f2 = p2.readMoveAsync();

        Step m1;
        Step m2;

        try {
            m1 = f1.get(MOVE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            m2 = f2.get(MOVE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            p1.send("Session timeout / error. Closing.");
            p2.send("Session timeout / error. Closing.");
            return null;
        }

        if (m1 == null || m2 == null) {
            if (m1 == null && m2 != null) {
                p2.send("Opponent disconnected. You WIN by default.");
            } else if (m2 == null && m1 != null) {
                p1.send("Opponent disconnected. You WIN by default.");
            }
            return null;
        }

        int cmp = Step.compare(m1, m2);
        if (cmp == 0) return RoundResult.draw(m1, m2);
        return (cmp > 0) ? RoundResult.p1wins(m1, m2) : RoundResult.p2wins(m1, m2);
    }

    private record RoundResult(boolean draw, boolean p1Wins, Step p1Step, Step p2Step) {
        static RoundResult draw(Step a, Step b) { return new RoundResult(true, false, a, b); }
        static RoundResult p1wins(Step a, Step b) { return new RoundResult(false, true, a, b); }
        static RoundResult p2wins(Step a, Step b) { return new RoundResult(false, false, a, b); }
    }
}
