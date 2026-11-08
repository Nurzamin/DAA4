package test;

import static org.junit.jupiter.api.Assertions.*;

import main.*;
import org.junit.jupiter.api.Test;

class LpsTest {
    private final KmpSearch kmp = new KmpSearch();

    @Test
    void referenceArrays() {
        assertArrayEquals(new int[]{0, 1, 2, 0, 1, 2, 3, 3}, kmp.lps("AAACAAAA"));
        assertArrayEquals(new int[]{0, 0, 1, 2, 0, 1, 2, 3, 4}, kmp.lps("ABABCABAB"));
    }

    @Test
    void smallCases() {
        assertArrayEquals(new int[]{0}, kmp.lps("A"));
        assertArrayEquals(new int[]{0, 0, 0}, kmp.lps("ABC"));
        assertArrayEquals(new int[]{0, 1, 2, 3}, kmp.lps("AAAA"));
        assertArrayEquals(new int[]{}, kmp.lps(""));
    }

    @Test
    void lpsBuildIsLinear() {
        String p = Generator.repeat('a', 999) + "b";
        kmp.lps(p);
        assertTrue(kmp.preprocessingComparisons() <= 2L * p.length());
    }
}
