# DAA Assignment 4 — Dynamic Programming and String Algorithms
**City Search & Spell-Check** · Student: *name surname, group* · Repo: *add GitHub link here (branch `main`, tag `v1.0`)*

Machine used for all runs: 1 vCPU virtualised Intel Xeon @ 2.1 GHz, OpenJDK 21, `-Xms2g -Xmx2g -XX:+UseSerialGC`. Raw data: `results/results.csv` (one row per timed run), aggregated: `results/summary.csv`.

## Experiment Design Card

> Written and committed **before the first run** of the experiment (see git history: `docs(card)` commit precedes `feat(exp)`).
> Everything runs from one fixed base seed (`Generator.BASE_SEED = 20261108`), so the same command gives the same strings and the same counters.

| Card field | What I wrote |
|---|---|
| **Question** | **Edit Distance:** (1) Does the recursive version really blow up exponentially, and where does it stop being usable? (2) Do Table and Two-rows grow like n·m as the theory says, and does the *input* change their cost at all? (3) How much memory does Two-rows really save? <br>**String search:** (4) Does Naive really degrade to n·m on a bad input while KMP stays linear? (5) On a realistic log file, is the difference even noticeable? |
| **Sizes (n)** | **ED, recursive (small-n experiment):** n = 3, 4, 5, … up to the point where the median time exceeds 3 s (a guard stops the loop – "nothing should run for an hour"). Expected stop around n ≈ 13–15. Table and Two-rows are also run on these sizes, so one plot can compare all three. <br>**ED, DP only (large-n experiment):** n = m = 250, 500, 1000, 2000, 4000 (16× range). 4000² = 16 M cells ≈ 0.02–0.1 s, 250² ≈ 0.1 ms, so the clock is never 0; the table at n = 4000 needs 64 MB, which still fits in a default JVM. <br>**Search, size sweep:** text length n = 100 000, 200 000, 400 000, 800 000, 1 600 000, 3 200 000 (32× range), pattern length m = 100. <br>**Search, pattern sweep:** n = 1 000 000 fixed, m = 10, 25, 50, 100, 200, 400 (40× range) – shows how the cost depends on m. |
| **Inputs** | **ED**: `random` – two independent random strings over {a,b,c,d} (the "average" case). `identical` – b = a (best case for the recursion: only the diagonal is walked; for the DP it shows that the table costs the same even when nothing needs fixing). `disjoint` – a over {a,b,c,d}, b over {w,x,y,z}: no equal characters, so every cell takes the 3-way branch → **worst case for recursion** (call count follows the Delannoy recurrence). `typo` – b = a with ~5 % random edits (what a real mistyped street name looks like; only in the large-n experiment and the small one, as a "realistic" input). <br>**Search**: `random` – random text over {a,b,c,d}, random pattern (average case). `worst_aab` – text aⁿ, pattern aᵐ⁻¹b: almost the whole pattern matches at every start, then fails → **worst case for Naive**; KMP is meant to be unaffected. `best_nofirst` – text aⁿ, pattern bᵐ: first character never matches → best case for Naive (≈ n comparisons). `log_lines` – synthetic server log (INFO/WARN/ERROR lines with near-miss lines), pattern `ERROR code=503 upstream timeout`: the realistic operator scenario. |
| **Metrics** | **ED:** `ops` = recursive calls (A) / table cells computed (B, C) counted inside the code – this is the cost the theory talks about (O(n·m) vs exponential). `mem_cells` = int cells allocated for DP storage – the one thing that differs between B and C. `time_ms` = median of 5 runs (3 if one run > 1.5 s) – to see the real effect, but it depends on the laptop and JIT, so it is never the only evidence. <br>**Search:** `ops` = character comparisons text-vs-pattern, `pre_ops` = comparisons spent building LPS (KMP only). Comparisons are exactly what separates Naive (re-checks characters) from KMP (never re-checks). `time_ms` = median of 5 runs. `result` = distance / number of matches, used to cross-check algorithms inside the experiment. |
| **Prediction** | **(1)** Recursive calls on `disjoint` grow by a factor ≈ 5.8 per +1 in n (Delannoy numbers: 1, 3, 13, 63, 321, …); time will follow the same curve, a straight line on a log axis. `random` (4 letters) grows more slowly but is still exponential; `identical` is linear (n+1 calls). The usable limit is n ≈ 13–15, stopped by *time*, not by the stack (depth is only n+m). <br>**(2)** `ops` for B and C = (n+1)(m+1) exactly, for **every** input type; time grows ×4 when n doubles and looks the same for all four input types. Two-rows will be about as fast as, or somewhat faster than, the table (less memory traffic). <br>**(3)** Memory: table (n+1)² cells vs Two-rows 2(n+1) cells – a ratio of about n/2, i.e. ≈ 2000× at n = 4000. <br>**(4)** Naive on `worst_aab` ≈ n·m = 100 comparisons per text character, KMP ≈ 2 per character (never more than 2n). On `random` Naive ≈ 1.33 per character, on `best_nofirst` exactly ≈ 1. Naive on `worst_aab` is tens of times slower than KMP in time (a bit less than the comparison ratio of ~50× because each comparison is very cheap). In the pattern sweep Naive's comparisons per character grow linearly with m, KMP's stay flat at ≈ 2. <br>**(5)** On `log_lines` Naive and KMP will be almost the same (≈ 1 comparison per char, almost no partial matches), so for real logs the worst case barely matters. |
| **Plots** | **P1** `ed_recursive_calls.png` – calls vs n, log y axis, three inputs + theory line: *does recursion explode, and which input is worst?* <br>**P2** `ed_time_all_versions.png` – median time vs n (log y) for recursive, table, two-rows: *where does recursion stop being usable while DP is still instant?* <br>**P3** `ed_dp_time_per_cell.png` – time / (n·m) in ns per cell vs n (ratio, log x): *is the DP really O(n·m), and does the input type matter?* <br>**P4** `ed_memory.png` – allocated cells vs n, log-log: *what do two rows save?* <br>**P5** `search_comparisons_vs_n.png` – comparisons per text character (ratio) vs n for each input, Naive vs KMP: *which input is the worst for Naive and does KMP stay flat?* <br>**P6** `search_time_vs_n.png` – median time vs n, log-log, 4 inputs × 2 algorithms: *does the counter picture show up on the clock?* <br>**P7** `search_pattern_length.png` – comparisons per char vs m on `worst_aab`: *how does the cost depend on the pattern length?* <br>**P8** `search_naive_vs_kmp_ratio.png` – bar chart of Naive/KMP time ratio per input at the largest n: *when is KMP actually worth it?* |

### Changes I made to the card after the first runs (and why)
1. **Random instances in the small-n experiment.** My first run used ONE random pair per n and the recursive curve was jagged (n=13 cheaper than n=12), because the cost of recursion on a single random pair is very erratic. Now every `random` point is 5 different pairs; plots show mean (calls) / median (time) with a min–max band.
2. **`typo` removed from the small-n experiment.** At n ≤ 20 a 5 % mutation usually changes nothing, so the input proved nothing. It stays in the large-n experiment.
3. **Per-configuration warm-up (≥ 50 ms).** The first run showed non-monotonic times on `log_lines` (0.77 ms at n=400k, 3.4 ms at n=100k): the JIT was recompiling when the input shape changed. On a 1-core machine the background compiler steals time from the measurement, so warm-up is time-based.
4. **Guard rule made concrete:** the loop over n stops after the first n where the slowest recursive run took > 1 s (the card said "median > 3 s"). The identical input never gets slow and runs to n = 40; plots show n ≤ 15.
5. The log pattern has m = 31 (not 100): it is the real error line, `ERROR code=503 upstream timeout`.

## Plots and results

### ED-1 · Recursion explodes
![ED-1](results/plots/ed_recursive_calls.png)
*Takeaway: on the worst input every +1 in n multiplies the work by ≈5.6 (n = 13 needs 2.1 billion calls), the best input needs only n+1 calls.* The black crosses are the reference recurrence `C(i,j) = 1 + C(i-1,j) + C(i,j-1) + C(i-1,j-1)`; the measured counter matches it exactly.

### ED-2 · Where recursion dies
![ED-2](results/plots/ed_time_all_versions.png)
*Takeaway: recursion needs 4.9 s for n = 13, while the table finishes n = 4000 in about 60 ms.*

### ED-3 · DP cost per cell
![ED-3](results/plots/ed_dp_time_per_cell.png)
*Takeaway: time per cell is flat in n (time ∝ n·m holds), but the disjoint input costs ≈1.9× more per cell than identical strings although the cell counts are equal.*

### ED-4 · Memory
![ED-4](results/plots/ed_memory.png)
*Takeaway: at n = 4000 two rows use 2,000× fewer cells than the table (8,002 cells ≈ 32 KB vs 16 M cells ≈ 64 MB).*

### S-1 · Comparisons per text character
![S-1](results/plots/search_comparisons_vs_n.png)
*Takeaway: Naive does ≈100 comparisons per character on the worst input and 1 on the best; KMP stays between 1 and 2 on every input; neither depends on n.*

### S-2 · Search time vs n
![S-2](results/plots/search_time_vs_n.png)
*Takeaway: Naive on its worst input is ≈56× slower than KMP on the same text (371 ms vs 7 ms at n = 3.2 M); the other lines grow roughly linearly, with visible noise on runs shorter than ~1 ms.*

### S-3 · Pattern length
![S-3](results/plots/search_pattern_length.png)
*Takeaway: Naive's cost per character equals m on the worst input (400 at m = 400), KMP stays at 2.0, so the longer the pattern the more KMP wins.*

### S-4 · Naive ÷ KMP per input
![S-4](results/plots/search_naive_vs_kmp_ratio.png)
*Takeaway: KMP wins big only on the adversarial input (56×); on random text and on a realistic log file the comparison counts differ by only 1.07× and 1.00×.*

## Prediction vs Result

**Edit Distance — recursion.** *Predicted:* ×5.8 per +1 in n on the worst input, limit n ≈ 13–15, stopped by time. *Saw:* ×5.6 (the counter equals the recurrence exactly: 377,393,953 calls at n = 12, 2,114,900,428 at n = 13); n = 12 took 0.8 s, n = 13 took 4.9 s. The small difference from 5.8 is because ×5.83 is the *asymptotic* rate (Delannoy numbers) and n ≤ 13 is still below it. *Surprise:* a single random pair is a poor benchmark — at n = 12 the cheapest of 5 random pairs needed 0.7 M calls and the most expensive 46 M (64× spread), and the n = 15 instances took between 0.09 and 0.49 billion calls. Testing only one random input would have hidden this.

**Edit Distance — DP.** *Predicted:* `ops` = (n+1)(m+1) for every input; time ×4 per doubling of n; same time for all inputs; two rows about as fast as or faster than the table; memory ratio ≈ n/2. *Saw:* the counter matches exactly (16,008,001 cells at n = 4000 for both versions) and memory is 16 M vs 8,002 cells (2,000×). Time grows ×3.5–×3.9 per doubling at n ≥ 1000 — as predicted. **Two predictions were wrong:** (a) the input *does* matter for time: disjoint strings cost ≈1.9× more per cell than identical ones, since a matching cell only copies a value while a mismatching cell computes a 3-way minimum with a dependency on the previous cell; the counter cannot see this. (b) Two rows was *not* consistently faster: it won on `disjoint` (≈51 vs 61 ms at n = 4000) and for small n, but lost on `random`/`identical` at n = 4000 (≈44–50 vs 33 ms). The error bars (±25 % on a shared 1-core VM) are as large as these differences and I did not find the cause, so I make no claim about which one is faster; the only solid difference is memory.

**String search.** *Predicted:* Naive worst ≈ n·m, KMP ≈ 2n, random ≈ 1.33n, best ≈ n, log ≈ n, flat per-character cost over n, linear-in-m growth for Naive. *Saw:* exactly that: 99.996 comparisons per char (Naive, worst), 1.9999 (KMP, worst — also the proven bound of 2n), 1.333 / 1.25 (Naive / KMP, random), 0.99997 (best), 1.003 / 1.0002 (log). Pattern sweep: Naive's comparisons per char are within 0.2 of m for all six m. *Slightly off:* I predicted the time ratio would be *a bit below* the comparison ratio (50×); it was 56×, because KMP's loop on aⁿ is extremely predictable and cheap. *Surprise:* on the clock KMP's slowest input was **random text** (16 ms for 4.0 M comparisons ≈ 4 ns each), not the worst case (6.7 ms for 6.4 M comparisons ≈ 1 ns each) — random data makes the branches unpredictable. Counters and time answer different questions.

## Short answers

**Which input made your string search slowest, and why?** `worst_aab` (text aⁿ, pattern aᵐ⁻¹b) made Naive slowest: 371 ms at n = 3.2 M and m = 100, with 320 M comparisons (99.996 per text character). At every start position the first 99 pattern characters match and the last one fails, so Naive throws the work away, moves one step and reads the same text characters again — about m comparisons per position, n·m in total. KMP on the same input needs 6.4 M comparisons (≤ 2n) because the LPS array tells it how much of the match is still valid. (On the clock KMP's own slowest input was random text, for branch-misprediction reasons, see above.)

**How large an n could the recursive Edit Distance handle, and what stopped it?** On the worst input (no equal characters) n = 12 took 0.8 s and n = 13 took 4.9 s with 2.1 billion calls; since the cost grows ×5.6 per character, n = 14 would need about 12 billion calls (≈ 25–30 s) and I stopped there. On random 4-letter strings it reached n = 15 (median 0.57 s, slowest instance 1.5 s), and on identical strings it handles any n because it only walks the diagonal. So the limit is not a number but depends on the input; for guaranteed behaviour on any input it is about n = 12. What stopped it was running time, not the stack: the recursion depth is only n + m = 26 frames, but the number of calls is exponential because the same f(i, j) is recomputed over and over.

**What do you gain and lose with two rows instead of the full table?** You gain memory: 2(m+1) cells instead of (n+1)(m+1), i.e. 32 KB instead of 64 MB at n = 4000 (2,000× less), which also means much better cache behaviour and the ability to run on strings that would not fit as a table. In my measurements it was not reliably faster (see above), so I would not claim a speed gain. You lose the table itself: the distance is correct, but you can no longer walk back through the table to recover *which* insertions, deletions and replacements were made (an alignment would need the full table or a more complicated divide-and-conquer such as Hirschberg's algorithm).

**Which string search for the city platform?** KMP. In my data it costs the same as Naive on random text and on a realistic log (comparison ratio 1.07× and 1.00×, clock ratio within noise), but it guarantees at most 2n comparisons for any text and any pattern, while Naive costs n·m in the bad case (56× slower at m = 100 and growing linearly with m, 682 ms vs 5.9 ms at m = 400). An operator pattern that repeats itself (e.g. a run of identical characters in a log) is exactly such a case, and the price of KMP is only one extra int array of length m. For short street names a simple `indexOf` would also be fine; I would choose KMP for the log scanning part, where texts are long and patterns are not under my control.

## One paragraph on time

Measured times ranged from 0.0001 ms (tiny DP tables) to 4.9 s (recursion, n = 13) and 371 ms (Naive worst case). Each is the median of 5 runs (3 for runs > 1.5 s), after a warm-up, with fast tasks timed in batches ≥ 2 ms so the clock never reads 0. Even so, the VM has one shared vCPU and the JIT competes with the measurement, so runs shorter than about 1 ms show visible noise (see the min–max bars in ED-3 and S-2), the two-rows vs table comparison is inside that noise, and the time per comparison varied from 1 ns to 6 ns depending on the input because of branch prediction. Therefore the times are not my only evidence: the counters (calls, cells, comparisons) are exact, identical on every run and independent of the laptop, and they explain *why* the curves look as they do; the clock is used to check that the counted work really is what costs time (it is, for recursion, for DP cells and for Naive's worst case) and to find the places where it is not the whole story (input-dependent cost per cell, random text for KMP).
