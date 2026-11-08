# DAA Assignment 4 — Edit Distance (DP) and String Search (Naive, KMP)

Scenario: City Search & Spell-Check. Report: [`REPORT.md`](REPORT.md). GitHub: *add link here* (branch `main`, tag `v1.0`).

## Layout
```
src/main/java/kz/city/search/   EditDistance (interface) + Recursive / Table / TwoRows,
                                StringSearch (interface) + NaiveSearch / KmpSearch,
                                Generator (seeded), Experiment (runner), SpellCheck (demo)
src/test/java/kz/city/search/   EditDistanceTest, LpsTest, SearchTest   (JUnit 5, 34 test runs)
scripts/plots.py                results.csv -> summary.csv + 8 PNG plots
results/results.csv             one row per timed run (columns below)
results/summary.csv             medians / means per configuration
results/plots/                  8 PNG plots
docs/experiment_card.md         Experiment Design Card (written before the first run)
```

## Build, test, run
Requirements: JDK 17+, Maven 3.8+, Python 3 with `pandas` and `matplotlib` (only for plots).
```bash
mvn test                      # build + all JUnit 5 tests
./run_experiment.sh           # compiles and writes results/results.csv (~2 min, deterministic counters)
python3 scripts/plots.py      # writes results/summary.csv and results/plots/*.png
java -cp target/classes kz.city.search.SpellCheck   # small spell-check demo
```
Reproducibility: all inputs come from `Generator` with the fixed base seed `20261108` (+ input tag + size), so the same command gives the same strings and exactly the same counters. Only the times change from machine to machine.

### results.csv columns
`experiment` (ed_small, ed_large, search_size, search_pattern), `family`, `algorithm`, `input`, `inst` (random instance index), `n`, `m`, `rep`, `time_ms`, `ops` (recursive calls / DP cells / character comparisons, counted inside the algorithm), `mem_cells` (allocated DP cells), `pre_ops` (LPS comparisons, KMP only), `result` (distance / number of matches).

## Design notes
* All three Edit Distance versions share `int distance(String, String)`; the state, transition and base case are written above each class.
* `KmpSearch.lps(String)` is public; the text index in `search` only increases; a test checks `comparisons <= 2n`.
* Counters are incremented inside the algorithms; tests check that they equal the expected values (e.g. Naive worst case = (n−m+1)·m).

## AI use acknowledgement
This project (code, tests, experiment, plots and report text) was produced with the help of an AI assistant (Claude, Anthropic) from the assignment PDF. I ran/reviewed the results and I am responsible for the submission; I can explain every part of the code and every plot. *(Edit this paragraph so it is true for you.)*
