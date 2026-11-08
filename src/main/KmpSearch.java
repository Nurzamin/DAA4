package main;

import java.util.ArrayList;
import java.util.List;

public final class KmpSearch implements StringSearch {
    private long comparisons;
    private long lpsComparisons;

    public int[] lps(String p) {
        lpsComparisons = 0;
        int m = p.length();
        int[] lps = new int[m];
        int len = 0; // length of the current matched prefix
        int i = 1;
        while (i < m) {
            lpsComparisons++;
            if (p.charAt(i) == p.charAt(len)) {
                lps[i++] = ++len;
            } else if (len > 0) {
                len = lps[len - 1]; // fall back inside the pattern, i does not move
            } else {
                lps[i++] = 0;
            }
        }
        return lps;
    }

    @Override
    public List<Integer> search(String text, String pattern) {
        if (pattern.isEmpty()) throw new IllegalArgumentException("pattern must not be empty");
        List<Integer> result = new ArrayList<>();
        int n = text.length(), m = pattern.length();
        int[] lps = lps(pattern); // resets lpsComparisons
        comparisons = 0;
        if (m > n) return result;
        int i = 0; // text index: never decreases
        int j = 0; // number of pattern chars currently matched
        while (i < n) {
            comparisons++; // one unit of work = one character comparison text-vs-pattern
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                if (j == m) {
                    result.add(i - m);
                    j = lps[j - 1]; // allows overlapping matches
                }
            } else if (j > 0) {
                j = lps[j - 1];
            } else {
                i++;
            }
        }
        return result;
    }

    @Override public String name() { return "kmp"; }
    @Override public long comparisons() { return comparisons; }
    @Override public long preprocessingComparisons() { return lpsComparisons; }
}
