package main;

import java.util.Random;

public final class Generator {
    public static final long BASE_SEED = 20261108L;

    private final Random rnd;

    public Generator(long seed) {
        this.rnd = new Random(seed);
    }

    /** Generator for one experiment configuration; String.hashCode is specified, so this is stable. */
    public static Generator forConfig(String tag, int n) {
        return new Generator(BASE_SEED * 1_000_003L + tag.hashCode() * 31L + n);
    }

    public String randomString(int n, String alphabet) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        return sb.toString();
    }

    public static String repeat(char c, int n) {
        char[] arr = new char[n];
        java.util.Arrays.fill(arr, c);
        return new String(arr);
    }

    /** Copy of s where each position is, with probability rate, replaced, deleted or followed by an insertion. */
    public String mutate(String s, double rate, String alphabet) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            if (rnd.nextDouble() >= rate) { sb.append(s.charAt(i)); continue; }
            switch (rnd.nextInt(3)) {
                case 0 -> sb.append(alphabet.charAt(rnd.nextInt(alphabet.length()))); // replace
                case 1 -> { /* delete */ }
                default -> { sb.append(s.charAt(i)); sb.append(alphabet.charAt(rnd.nextInt(alphabet.length()))); } // insert
            }
        }
        return sb.toString();
    }

    public static final String ERROR_LINE = "ERROR code=503 upstream timeout";

    /**
     * Synthetic server log of exactly n characters. Mostly INFO lines, some near-miss lines
     * (ERROR code=500, WARN code=503) and rare real ERROR_LINE lines.
     */
    public String logText(int n) {
        String[] services = {"auth", "maps", "bus", "parking", "search", "billing"};
        String[] msgs = {"request ok", "cache hit", "user login", "route computed", "payment accepted"};
        StringBuilder sb = new StringBuilder(n + 128);
        int sec = 0;
        while (sb.length() < n) {
            sec++;
            sb.append("2026-11-08 ")
              .append(String.format("%02d:%02d:%02d", (sec / 3600) % 24, (sec / 60) % 60, sec % 60))
              .append(' ');
            double r = rnd.nextDouble();
            if (r < 0.002) sb.append(ERROR_LINE);
            else if (r < 0.01) sb.append("ERROR code=500 internal failure");
            else if (r < 0.03) sb.append("WARN code=503 retrying");
            else sb.append("INFO svc=").append(services[rnd.nextInt(services.length)])
                   .append(" msg=").append(msgs[rnd.nextInt(msgs.length)]);
            sb.append('\n');
        }
        sb.setLength(n);
        return sb.toString();
    }
}
