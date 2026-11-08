package main;

public final class EditDistanceTable implements EditDistance {
    private long cells;
    private long mem;

    @Override
    public int distance(String a, String b) {
        int n = a.length(), m = b.length();
        int[][] d = new int[n + 1][m + 1];
        cells = 0;
        mem = (long) (n + 1) * (m + 1);
        for (int i = 0; i <= n; i++) { d[i][0] = i; cells++; }
        for (int j = 1; j <= m; j++) { d[0][j] = j; cells++; }
        for (int i = 1; i <= n; i++) {
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                cells++; // one unit of work = one table cell computed
                if (ca == b.charAt(j - 1)) {
                    d[i][j] = d[i - 1][j - 1];
                } else {
                    d[i][j] = 1 + Math.min(d[i - 1][j], Math.min(d[i][j - 1], d[i - 1][j - 1]));
                }
            }
        }
        return d[n][m];
    }

    @Override public String name() { return "table"; }
    @Override public long operations() { return cells; }
    @Override public long memoryCells() { return mem; }
}
