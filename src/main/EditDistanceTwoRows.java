package main;

public final class EditDistanceTwoRows implements EditDistance {
    private long cells;
    private long mem;

    @Override
    public int distance(String a, String b) {
        int n = a.length(), m = b.length();
        int[] prev = new int[m + 1];
        int[] cur = new int[m + 1];
        cells = 0;
        mem = 2L * (m + 1);
        for (int j = 0; j <= m; j++) { prev[j] = j; cells++; }
        for (int i = 1; i <= n; i++) {
            cur[0] = i;
            cells++;
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                cells++; // one unit of work = one table cell computed
                if (ca == b.charAt(j - 1)) {
                    cur[j] = prev[j - 1];
                } else {
                    cur[j] = 1 + Math.min(prev[j], Math.min(cur[j - 1], prev[j - 1]));
                }
            }
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[m];
    }

    @Override public String name() { return "two_rows"; }
    @Override public long operations() { return cells; }
    @Override public long memoryCells() { return mem; }
}
