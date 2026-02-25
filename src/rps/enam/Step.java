package rps.enam;

import java.util.Locale;

public enum Step {
    ROCK, PAPER, SCISSORS;

    public static Step parse(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase(Locale.ROOT);

        // English
        switch (s) {
            case "rock":
            case "r":
                return ROCK;
            case "paper":
            case "p":
                return PAPER;
            case "scissors":
            case "scissor":
            case "s":
                return SCISSORS;
        }

        // Russian
        switch (s) {
            case "камень":
            case "к":
                return ROCK;
            case "бумага":
            case "б":
                return PAPER;
            case "ножницы":
            case "н":
                return SCISSORS;
        }

        return null;
    }

    public static int compare(Step a, Step b) {
        if (a == b) return 0;
        // rock beats scissors, scissors beats paper, paper beats rock
        return switch (a) {
            case ROCK -> (b == SCISSORS) ? 1 : -1;
            case PAPER -> (b == ROCK) ? 1 : -1;
            case SCISSORS -> (b == PAPER) ? 1 : -1;
        };
    }

}
