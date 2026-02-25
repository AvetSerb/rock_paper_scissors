package rps.server;

public class Arguments {
    private Arguments() {
    }

    public static int parsePort(String[] args, int defaultPort) {
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                return parseInt(args[i + 1], defaultPort);
            }
        }
        return defaultPort;
    }

    private static int parseInt(String s, int fallback) {
        try {
            int v = Integer.parseInt(s);
            if (v < 1 || v > 65535) return fallback;
            return v;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

}
