package main;

public final class EditDistanceRecursive implements EditDistance {
    private long calls;

    @Override
    public int distance(String a, String b) {
        calls = 0;
        return f(a, b, a.length(), b.length());
    }

    private int f(String a, String b, int i, int j) {
        calls++; // one unit of work = one call
        if (i == 0) return j;
        if (j == 0) return i;
        if (a.charAt(i - 1) == b.charAt(j - 1)) {
            return f(a, b, i - 1, j - 1);
        }
        int del = f(a, b, i - 1, j);
        int ins = f(a, b, i, j - 1);
        int rep = f(a, b, i - 1, j - 1);
        return 1 + Math.min(del, Math.min(ins, rep));
    }

    @Override public String name() { return "recursive"; }
    @Override public long operations() { return calls; }
    @Override public long memoryCells() { return 0; } // only the call stack (depth <= n + m)
}
