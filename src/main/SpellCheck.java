package main;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SpellCheck {
    private static final EditDistance ED = new EditDistanceTwoRows();

    public static List<String> suggest(String query, List<String> dictionary, int k) {
        String q = query.toLowerCase(Locale.ROOT);
        List<String> sorted = new ArrayList<>(dictionary);
        sorted.sort(Comparator.comparingInt((String s) -> ED.distance(q, s.toLowerCase(Locale.ROOT)))
                .thenComparing(Comparator.naturalOrder()));
        return sorted.subList(0, Math.min(k, sorted.size()));
    }

    public static void main(String[] args) {
        List<String> streets = List.of("Abylai Khan", "Kenesary", "Tauelsizdik", "Respublika",
                "Kabanbay Batyr", "Mangilik El", "Saryarka", "Turan", "Kunayev", "Dostyk");
        for (String q : new String[]{"Abylai Han", "Kenesary", "Mangilic El", "Sarjarka"}) {
            System.out.printf("%-14s -> %s%n", q, suggest(q, streets, 3));
        }
        System.out.println("search(log, ERROR) demo: "
                + new KmpSearch().search("a ERROR b ERROR", "ERROR"));
    }
}
