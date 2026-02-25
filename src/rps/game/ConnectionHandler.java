package rps.game;

import rps.players.Player;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class ConnectionHandler implements Runnable {
    private final Socket socket;
    private final MatchMaker matchMaker;

    public ConnectionHandler(Socket socket, MatchMaker matchMaker) {
        this.socket = socket;
        this.matchMaker = matchMaker;
    }

    @Override
    public void run() {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            out.println("Welcome to Rock-Paper-Scissors! / Добро пожаловать в (Камень/Ножницы/Бумага)");
            out.println("Enter your nickname: nc localhost 2323/ Введите имя: ");

            String nick = readNonEmptyLine(in);
            if (nick == null) return;

            try {
                validateNickname(nick.trim());
            } catch (InvalidNicknameException e) {
                out.println("Error: " + e.getMessage() + ". Please reconnect with a valid nickname.");
                return;
            }

            Player player = new Player(nick.trim(), in, out, socket);
            out.println("Hi, " + player.nick() + "! Waiting for an opponent...");

            matchMaker.enqueue(player);

            while (!socket.isClosed()) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (IOException e) {
        }
    }

    public static class InvalidNicknameException extends Exception {
        public InvalidNicknameException(String message) {
            super(message);
        }
    }

    private static void validateNickname(String nick) throws InvalidNicknameException {
        if (nick.matches(".*[.,!?;:\"'()\\[\\]{}\\\\/].*")) {
            throw new InvalidNicknameException("Nickname cannot contain punctuation marks");
        }
    }

    private static String readNonEmptyLine(BufferedReader in) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            line = line.strip();
            if (!line.isEmpty()) return line;
        }
        return null;
    }

}
