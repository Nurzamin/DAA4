package main;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class Experiment {
    static final String ALPHA = "abcd";
    static final String ALPHA_OTHER = "wxyz";
    static final int REPS = 5;
    static final double PROCEED_LIMIT_MS = 1000;
    static final double SLOW_RUN_MS = 1500;
    static final int RANDOM_INSTANCES = 5;

    private final PrintWriter out;

    private Experiment(PrintWriter out) { this.out = out; }

    public static void main(String[] args) throws IOException {
        Path dir = Path.of(args.length > 0 ? args[0] : "results");
        Files.createDirectories(dir);
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(dir.resolve("results.csv")))) {
            pw.println("experiment,family,algorithm,input,inst,n,m,rep,time_ms,ops,mem_cells,pre_ops,result");
            Experiment e = new Experiment(pw);
            warmUp();
            e.editSmall();
            e.editLarge();
            e.searchSizeSweep();
            e.searchPatternSweep();
        }
        System.out.println("Done: " + dir.resolve("results.csv"));
    }

    static String[] editInput(String type, int n, int inst) {
        Generator g = Generator.forConfig("ed_" + type + "#" + inst, n);
        String a = g.randomString(n, ALPHA);
        return switch (type) {
            case "random" -> new String[]{a, g.randomString(n, ALPHA)};
            case "identical" -> new String[]{a, a};
            case "disjoint" -> new String[]{a, g.randomString(n, ALPHA_OTHER)};
            case "typo" -> new String[]{a, g.mutate(a, 0.05, ALPHA)};
            default -> throw new IllegalArgumentException(type);
        };
    }

    private void editSmall() {
        String[] inputs = {"random", "identical", "disjoint"};
        for (String type : inputs) {
            int instances = type.equals("random") ? RANDOM_INSTANCES : 1;
            for (int n = 3; n <= 40; n++) {
                double slowestRecursive = 0;
                for (int inst = 0; inst < instances; inst++) {
                    String[] ab = editInput(type, n, inst);
                    int expected = -1;
                    for (Supplier<EditDistance> s : edSuppliers()) {
                        EditDistance alg = s.get();
                        double median = runEdit("ed_small", alg, type, inst, ab, n);
                        int d = alg.distance(ab[0], ab[1]);
                        if (expected < 0) expected = d;
                        if (d != expected) throw new IllegalStateException("versions disagree: " + type + " n=" + n);
                        if (alg instanceof EditDistanceRecursive) slowestRecursive = Math.max(slowestRecursive, median);
                    }
                }
                if (slowestRecursive > PROCEED_LIMIT_MS) break;
            }
        }
    }

    private void editLarge() {
        int[] sizes = {250, 500, 1000, 2000, 4000};
        String[] inputs = {"random", "identical", "disjoint", "typo"};
        for (String type : inputs) {
            for (int n : sizes) {
                String[] ab = editInput(type, n, 0);
                int d1 = -1;
                for (Supplier<EditDistance> s : edSuppliers()) {
                    EditDistance alg = s.get();
                    if (alg instanceof EditDistanceRecursive) continue;
                    runEdit("ed_large", alg, type, 0, ab, n);
                    int d = alg.distance(ab[0], ab[1]);
                    if (d1 < 0) d1 = d;
                    if (d != d1) throw new IllegalStateException("versions disagree: " + type + " n=" + n);
                }
            }
        }
    }

    private static List<Supplier<EditDistance>> edSuppliers() {
        return List.of(EditDistanceRecursive::new, EditDistanceTable::new, EditDistanceTwoRows::new);
    }

    private double runEdit(String experiment, EditDistance alg, String type, int inst, String[] ab, int n) {
        int result = alg.distance(ab[0], ab[1]);   // counted run (counters are reset inside distance)
        long ops = alg.operations(), mem = alg.memoryCells();
        double[] times = timeRuns(() -> alg.distance(ab[0], ab[1]));
        for (int r = 0; r < times.length; r++) {
            row(experiment, "edit", alg.name(), type, inst, n, ab[1].length(), r + 1, times[r], ops, mem, 0, result);
        }
        return median(times);
    }

    private void searchSizeSweep() {
        int m = 100;
        int[] sizes = {100_000, 200_000, 400_000, 800_000, 1_600_000, 3_200_000};
        String[] inputs = {"random", "worst_aab", "best_nofirst", "log_lines"};
        for (String type : inputs) {
            for (int n : sizes) {
                String[] tp = searchInput(type, n, m);
                runSearchBoth("search_size", type, tp[0], tp[1]);
            }
        }
    }

    private void searchPatternSweep() {
        int n = 1_000_000;
        for (int m : new int[]{10, 25, 50, 100, 200, 400}) {
            String[] tp = searchInput("worst_aab", n, m);
            runSearchBoth("search_pattern", "worst_aab", tp[0], tp[1]);
        }
    }

    static String[] searchInput(String type, int n, int m) {
        Generator g = Generator.forConfig("search_" + type, n * 1000 + m);
        return switch (type) {
            case "random" -> new String[]{g.randomString(n, ALPHA), g.randomString(m, ALPHA)};
            case "worst_aab" -> new String[]{Generator.repeat('a', n), Generator.repeat('a', m - 1) + "b"};
            case "best_nofirst" -> new String[]{Generator.repeat('a', n), Generator.repeat('b', m)};
            case "log_lines" -> new String[]{g.logText(n), Generator.ERROR_LINE};
            default -> throw new IllegalArgumentException(type);
        };
    }

    private void runSearchBoth(String experiment, String type, String text, String pattern) {
        List<Integer> expected = null;
        for (StringSearch alg : List.of(new NaiveSearch(), new KmpSearch())) {
            List<Integer> res = alg.search(text, pattern);       // counted run
            long ops = alg.comparisons(), pre = alg.preprocessingComparisons();
            if (expected == null) expected = res;
            else if (!expected.equals(res)) throw new IllegalStateException("naive and kmp disagree: " + type);
            double[] times = timeRuns(() -> alg.search(text, pattern));
            for (int r = 0; r < times.length; r++) {
                row(experiment, "search", alg.name(), type, 0, text.length(), pattern.length(),
                        r + 1, times[r], ops, 0, pre, res.size());
            }
        }
    }

    static double[] timeRuns(Runnable task) {
        long t0 = System.nanoTime();
        task.run();
        double first = (System.nanoTime() - t0) / 1e6;
        if (first < 100) {
            long wStart = System.nanoTime();
            for (int w = 0; w < 500 && System.nanoTime() - wStart < 50_000_000L; w++) task.run();
        }
        int batch = first >= 2.0 ? 1 : (int) Math.min(100_000, Math.ceil(2.0 / Math.max(first, 0.00005)));
        int reps = first > SLOW_RUN_MS ? 3 : REPS;
        double[] times = new double[reps];
        for (int r = 0; r < reps; r++) {
            long s = System.nanoTime();
            for (int b = 0; b < batch; b++) task.run();
            times[r] = (System.nanoTime() - s) / 1e6 / batch;
        }
        return times;
    }

    static double median(double[] a) {
        double[] c = a.clone();
        Arrays.sort(c);
        return c.length % 2 == 1 ? c[c.length / 2] : (c[c.length / 2 - 1] + c[c.length / 2]) / 2;
    }

    static void warmUp() {
        Generator g = new Generator(1);
        List<EditDistance> eds = List.of(new EditDistanceTable(), new EditDistanceTwoRows());
        String a = g.randomString(300, ALPHA), b = g.randomString(300, ALPHA);
        for (int i = 0; i < 300; i++) for (EditDistance e : eds) e.distance(a, b);
        EditDistanceRecursive rec = new EditDistanceRecursive();
        for (int i = 0; i < 20; i++) rec.distance(g.randomString(8, ALPHA), g.randomString(8, ALPHA));
        String t = g.randomString(100_000, ALPHA), p = g.randomString(50, ALPHA);
        for (int i = 0; i < 30; i++) { new NaiveSearch().search(t, p); new KmpSearch().search(t, p); }
    }

    private void row(String exp, String family, String alg, String input, int inst, int n, int m, int rep,
                     double timeMs, long ops, long mem, long pre, long result) {
        out.println(String.join(",", exp, family, alg, input, String.valueOf(inst), String.valueOf(n), String.valueOf(m),
                String.valueOf(rep), String.format(Locale.ROOT, "%.6f", timeMs),
                String.valueOf(ops), String.valueOf(mem), String.valueOf(pre), String.valueOf(result)));
        out.flush();
    }
}
