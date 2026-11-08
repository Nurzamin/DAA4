package test;

import static org.junit.jupiter.api.Assertions.*;

import  main.*;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SearchTest {

    static Stream<Supplier<StringSearch>> impls() {
        return Stream.of(NaiveSearch::new, KmpSearch::new);
    }

    @ParameterizedTest @MethodSource("impls")
    void referenceMatches(Supplier<StringSearch> s) {
        StringSearch ss = s.get();
        assertEquals(List.of(10), ss.search("ABABDABACDABABCABAB", "ABABCABAB"));
        assertEquals(List.of(0, 1, 2, 3), ss.search("AAAAA", "AA"));
        assertEquals(List.of(0, 2, 4), ss.search("ABABABA", "ABA"));
    }

    @ParameterizedTest @MethodSource("impls")
    void noMatchGivesEmptyList(Supplier<StringSearch> s) {
        assertEquals(List.of(), s.get().search("ABCDEF", "XYZ"));
    }

    @ParameterizedTest @MethodSource("impls")
    void emptyPatternThrows(Supplier<StringSearch> s) {
        assertThrows(IllegalArgumentException.class, () -> s.get().search("abc", ""));
        assertThrows(IllegalArgumentException.class, () -> s.get().search("", ""));
    }

    @ParameterizedTest @MethodSource("impls")
    void patternLongerThanTextGivesEmptyList(Supplier<StringSearch> s) {
        assertEquals(List.of(), s.get().search("abc", "abcd"));
        assertEquals(List.of(), s.get().search("", "a"));
    }

    @ParameterizedTest @MethodSource("impls")
    void patternEqualsText(Supplier<StringSearch> s) {
        assertEquals(List.of(0), s.get().search("abc", "abc"));
    }

    @ParameterizedTest @MethodSource("impls")
    void matchesAtBothEnds(Supplier<StringSearch> s) {
        assertEquals(List.of(0, 4), s.get().search("abcxabc", "abc"));
    }

    @Test
    void naiveAndKmpAgreeOn100RandomTexts() {
        Random rnd = new Random(5);
        Generator g = new Generator(5);
        NaiveSearch naive = new NaiveSearch();
        KmpSearch kmp = new KmpSearch();
        for (int t = 0; t < 100; t++) {
            String text = g.randomString(rnd.nextInt(60), "ab");
            String pat = g.randomString(1 + rnd.nextInt(5), "ab");
            assertEquals(naive.search(text, pat), kmp.search(text, pat), text + " / " + pat);
        }
    }

    @Test
    void kmpNeverMovesBackwardsSoItDoesAtMost2nComparisons() {
        Generator g = new Generator(9);
        KmpSearch kmp = new KmpSearch();
        String[][] cases = {
            {Generator.repeat('a', 5000), Generator.repeat('a', 99) + "b"},
            {g.randomString(5000, "ab"), "abababababab"},
            {Generator.repeat('a', 5000), Generator.repeat('a', 50)},
        };
        for (String[] c : cases) {
            kmp.search(c[0], c[1]);
            assertTrue(kmp.comparisons() <= 2L * c[0].length(),
                    "comparisons " + kmp.comparisons() + " for n=" + c[0].length());
        }
    }

    @Test
    void naiveWorstCaseCountsExactlyNMinusMPlus1TimesM() {
        int n = 1000, m = 50;
        NaiveSearch naive = new NaiveSearch();
        naive.search(Generator.repeat('a', n), Generator.repeat('a', m - 1) + "b");
        assertEquals((long) (n - m + 1) * m, naive.comparisons());
    }

    @Test
    void naiveBestCaseIsLinear() {
        int n = 1000, m = 50;
        NaiveSearch naive = new NaiveSearch();
        naive.search(Generator.repeat('a', n), Generator.repeat('b', m));
        assertEquals(n - m + 1, naive.comparisons());
    }

    @Test
    void logSearchFindsEveryErrorLine() {
        String log = Generator.forConfig("log", 20000).logText(20000);
        List<Integer> hits = new KmpSearch().search(log, Generator.ERROR_LINE);
        assertEquals(new NaiveSearch().search(log, Generator.ERROR_LINE), hits);
        for (int h : hits) assertEquals(Generator.ERROR_LINE, log.substring(h, h + Generator.ERROR_LINE.length()));
    }
}
