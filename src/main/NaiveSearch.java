package main;

import java.util.ArrayList;
import java.util.List;

public final class NaiveSearch implements StringSearch {
    private long comparisons;

    @Override
    public List<Integer> search(String text, String pattern) {
        if (pattern.isEmpty()) throw new IllegalArgumentException("pattern must not be empty");
        comparisons = 0;
        List<Integer> result = new ArrayList<>();
        int n = text.length(), m = pattern.length();
        for (int s = 0; s + m <= n; s++) {
            int k = 0;
            while (k < m) {
                comparisons++; // one unit of work = one character comparison
                if (text.charAt(s + k) != pattern.charAt(k)) break;
                k++;
            }
            if (k == m) result.add(s);
        }
        return result;
    }

    @Override public String name() { return "naive"; }
    @Override public long comparisons() { return comparisons; }
    @Override public long preprocessingComparisons() { return 0; }
}
