package test;

import static org.junit.jupiter.api.Assertions.*;

import main.*;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class EditDistanceTest {

    static Stream<Supplier<EditDistance>> impls() {
        return Stream.of(EditDistanceRecursive::new, EditDistanceTable::new, EditDistanceTwoRows::new);
    }

    @ParameterizedTest @MethodSource("impls")
    void referenceValues(Supplier<EditDistance> s) {
        EditDistance ed = s.get();
        assertEquals(3, ed.distance("kitten", "sitting"));
        assertEquals(2, ed.distance("flaw", "lawn"));
        assertEquals(5, ed.distance("intention", "execution"));
        assertEquals(3, ed.distance("", "abc"));
        assertEquals(0, ed.distance("abc", "abc"));
    }

    @ParameterizedTest @MethodSource("impls")
    void emptyStrings(Supplier<EditDistance> s) {
        EditDistance ed = s.get();
        assertEquals(0, ed.distance("", ""));
        assertEquals(3, ed.distance("abc", ""));
        assertEquals(3, ed.distance("", "abc"));
    }

    @ParameterizedTest @MethodSource("impls")
    void cityScenario(Supplier<EditDistance> s) {
        assertEquals(1, s.get().distance("abylai han", "abylai khan"));
    }

    @Test
    void threeVersionsAgreeOn100RandomShortStrings() {
        Random rnd = new Random(7);
        Generator g = new Generator(7);
        EditDistance a = new EditDistanceRecursive(), b = new EditDistanceTable(), c = new EditDistanceTwoRows();
        for (int t = 0; t < 100; t++) {
            String x = g.randomString(rnd.nextInt(8), "abc");
            String y = g.randomString(rnd.nextInt(8), "abc");
            int expected = b.distance(x, y);
            assertEquals(expected, a.distance(x, y), x + " / " + y);
            assertEquals(expected, c.distance(x, y), x + " / " + y);
            assertEquals(expected, b.distance(y, x), "symmetry " + x + " / " + y);
        }
    }

    @Test
    void tableAndTwoRowsAgreeOnLongStrings() {
        Generator g = new Generator(11);
        String x = g.randomString(300, "abcd");
        String y = g.mutate(x, 0.1, "abcd");
        assertEquals(new EditDistanceTable().distance(x, y), new EditDistanceTwoRows().distance(x, y));
    }

    @Test
    void countersAreMeasuredInsideTheAlgorithms() {
        String a = "abcdef", b = "uvwxyz1"; // n = 6, m = 7, no common characters
        EditDistance table = new EditDistanceTable(), two = new EditDistanceTwoRows(), rec = new EditDistanceRecursive();
        table.distance(a, b); two.distance(a, b); rec.distance(a, b);
        assertEquals(7 * 8, table.memoryCells());
        assertEquals(2 * 8, two.memoryCells());
        assertEquals(7 * 8, table.operations());
        assertEquals(7 * 8, two.operations());
        // recursive calls on disjoint strings: C(i,j) = 1 + C(i-1,j) + C(i,j-1) + C(i-1,j-1), C(0,*) = C(*,0) = 1
        assertEquals(calls(6, 7), rec.operations());
        // identical strings: only the diagonal is walked, n + 1 calls
        rec.distance("abcdef", "abcdef");
        assertEquals(7, rec.operations());
    }

    private static long calls(int i, int j) {
        if (i == 0 || j == 0) return 1;
        return 1 + calls(i - 1, j) + calls(i, j - 1) + calls(i - 1, j - 1);
    }

    @Test
    void generatorIsReproducible() {
        assertEquals(Generator.forConfig("x", 100).randomString(100, "abcd"),
                     Generator.forConfig("x", 100).randomString(100, "abcd"));
        assertEquals(Generator.forConfig("log", 500).logText(500), Generator.forConfig("log", 500).logText(500));
        assertEquals(500, Generator.forConfig("log", 500).logText(500).length());
    }

    @Test
    void spellCheckSuggestsClosestStreet() {
        List<String> dict = List.of("Abylai Khan", "Kenesary", "Turan");
        assertEquals("Abylai Khan", SpellCheck.suggest("Abylai Han", dict, 1).get(0));
    }
}
