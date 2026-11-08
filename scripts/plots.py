#!/usr/bin/env python3

import os
import textwrap

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from matplotlib.ticker import FuncFormatter

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "results")
OUT = os.path.join(RES, "plots")
os.makedirs(OUT, exist_ok=True)

# Okabe-Ito colour-blind-safe palette
C = {"blue": "#0072B2", "orange": "#E69F00", "green": "#009E73", "red": "#D55E00",
     "purple": "#CC79A7", "sky": "#56B4E9", "black": "#222222", "grey": "#888888"}
INPUT_COLOR = {"random": C["blue"], "identical": C["green"], "disjoint": C["red"], "typo": C["purple"],
               "worst_aab": C["red"], "best_nofirst": C["green"], "log_lines": C["purple"]}
INPUT_LABEL = {"random": "random", "identical": "identical (best for recursion)",
               "disjoint": "disjoint alphabets (worst for recursion)", "typo": "typo, ~5% edits",
               "worst_aab": "worst case: aⁿ / aᵐ⁻¹b", "best_nofirst": "best case: aⁿ / bᵐ",
               "log_lines": "server log, pattern ERROR code=503…"}

plt.rcParams.update({
    "figure.dpi": 100, "savefig.dpi": 170, "font.size": 11, "axes.titlesize": 13, "axes.titleweight": "bold",
    "axes.labelsize": 11, "axes.grid": True, "grid.alpha": 0.3, "axes.spines.top": False,
    "axes.spines.right": False, "legend.frameon": False, "font.family": "DejaVu Sans",
})


def plain(x, _pos=None):
    return f"{x:,.0f}" if x >= 1 else f"{x:g}"


def save(fig, name, takeaway):
    fig.text(0.5, -0.025, "\n".join(textwrap.wrap("▶ " + takeaway, 105)), ha="center", va="top",
             fontsize=10, style="italic", color=C["black"])
    fig.savefig(os.path.join(OUT, name), bbox_inches="tight", facecolor="white")
    plt.close(fig)
    print("wrote", name)


# ------------------------------------------------------------------ load + summarise
raw = pd.read_csv(os.path.join(RES, "results.csv"))
per_inst = (raw.groupby(["experiment", "family", "algorithm", "input", "inst", "n", "m"])
            .agg(time_ms=("time_ms", "median"), t_min=("time_ms", "min"), t_max=("time_ms", "max"),
                 runs=("rep", "count"), ops=("ops", "first"), mem_cells=("mem_cells", "first"),
                 pre_ops=("pre_ops", "first"), result=("result", "first")).reset_index())
summary = (per_inst.groupby(["experiment", "family", "algorithm", "input", "n", "m"])
           .agg(instances=("inst", "count"), time_ms=("time_ms", "median"), t_min=("t_min", "min"),
                t_max=("t_max", "max"), ops_mean=("ops", "mean"), ops_min=("ops", "min"), ops_max=("ops", "max"),
                mem_cells=("mem_cells", "first"), pre_ops=("pre_ops", "first"), result=("result", "first"))
           .reset_index())
summary.to_csv(os.path.join(RES, "summary.csv"), index=False, float_format="%.6g")

small = per_inst[per_inst.experiment == "ed_small"]
large = per_inst[per_inst.experiment == "ed_large"]
ssize = per_inst[per_inst.experiment == "search_size"]
spat = per_inst[per_inst.experiment == "search_pattern"]


def delannoy_calls(n):
    c = np.ones((n + 1, n + 1), dtype=object)
    for i in range(1, n + 1):
        for j in range(1, n + 1):
            c[i][j] = 1 + c[i - 1][j] + c[i][j - 1] + c[i - 1][j - 1]
    return int(c[n][n])

fig, ax = plt.subplots(figsize=(9, 5.4))
rec = small[small.algorithm == "recursive"]
d = rec[rec.input == "disjoint"].sort_values("n")
ax.plot(d.n, d.ops, "o-", color=C["red"], lw=2, ms=6, label=INPUT_LABEL["disjoint"], zorder=3)
nn = np.arange(3, int(d.n.max()) + 1)
ax.plot(nn, [delannoy_calls(int(k)) for k in nn], linestyle="none", marker="x", color=C["black"], ms=8, mew=1.6,
        label="reference recurrence C(i,j)=1+C(i−1,j)+C(i,j−1)+C(i−1,j−1)", zorder=5)
r = rec[rec.input == "random"].groupby("n").ops.agg(["mean", "min", "max"]).reset_index()
ax.plot(r.n, r["mean"], "s-", color=C["blue"], lw=2, ms=5, label="random, mean of 5 instances")
ax.fill_between(r.n, r["min"], r["max"], color=C["blue"], alpha=0.18, label="random, min–max of 5 instances")
i = rec[rec.input == "identical"].sort_values("n")
ax.plot(i.n[i.n <= 15], i.ops[i.n <= 15], "^-", color=C["green"], lw=2, ms=5, label=INPUT_LABEL["identical"])
ax.set_yscale("log")
ax.set_xlabel("string length n (= m), characters")
ax.set_ylabel("recursive calls (count, log scale)")
ax.set_title("Recursive edit distance: calls explode exponentially")
ax.legend(loc="upper left", fontsize=9)
growth = d.ops.iloc[-1] / d.ops.iloc[-2]
save(fig, "ed_recursive_calls.png",
     f"On the worst input every +1 in n multiplies the work by about {growth:.1f} (n=13 needs "
     f"{d.ops.iloc[-1] / 1e9:.1f} billion calls), while the best input needs only n+1 calls.")

# ================================================================== P2 time of all versions
fig, ax = plt.subplots(figsize=(9, 5.4))
dis = per_inst[(per_inst.input == "disjoint") & per_inst.experiment.isin(["ed_small", "ed_large"])]
for alg, col, mk, lab in [("recursive", C["red"], "o", "recursive (A)"), ("table", C["orange"], "s", "full table (B)"),
                          ("two_rows", C["blue"], "^", "two rows (C)")]:
    for k, part in enumerate([dis[dis.experiment == "ed_small"], dis[dis.experiment == "ed_large"]]):
        s = part[part.algorithm == alg].sort_values("n")   # two separate segments: no line across the gap
        if len(s):
            ax.plot(s.n, s.time_ms, marker=mk, color=col, lw=2, ms=6, label=(lab + ", disjoint input") if k == 0 or alg != "recursive" and k == 0 else None)
rr = small[(small.algorithm == "recursive") & (small.input == "random")].groupby("n").time_ms.median().reset_index()
ax.plot(rr.n, rr.time_ms, "o--", color=C["red"], alpha=0.5, lw=1.4, ms=4, label="recursive (A), random input (median of 5)")
ax.axhline(1000, color=C["grey"], lw=1, ls=":")
ax.text(4500, 1250, "1 second", color=C["grey"], fontsize=9, ha="right")
ax.set_xscale("log"); ax.set_yscale("log")
ax.xaxis.set_major_formatter(FuncFormatter(plain)); ax.yaxis.set_major_formatter(FuncFormatter(lambda v, _: f"{v:g}"))
ax.set_xlabel("string length n (= m), characters (log scale)")
ax.set_ylabel("median time per call, ms (log scale)")
ax.set_title("Where recursion dies and the DP is still instant")
ax.legend(loc="lower right", fontsize=9)
t_rec = dis[(dis.algorithm == "recursive") & (dis.n == 13)].time_ms.iloc[0]
t_tab = dis[(dis.algorithm == "table") & (dis.n == 4000)].time_ms.iloc[0]
save(fig, "ed_time_all_versions.png",
     f"Recursion needs {t_rec / 1000:.1f} s for n=13, while the table finishes n=4000 (300× longer strings) "
     f"in {t_tab:.0f} ms.")

# ================================================================== P3 DP time per cell
fig, axes = plt.subplots(1, 2, figsize=(10.5, 5.2), sharey=True)
for ax, alg, title in [(axes[0], "table", "full table (B)"), (axes[1], "two_rows", "two rows (C)")]:
    for inp in ["random", "identical", "disjoint", "typo"]:
        s = large[(large.algorithm == alg) & (large.input == inp)].sort_values("n")
        y = s.time_ms * 1e6 / s.ops
        lo = s.t_min * 1e6 / s.ops; hi = s.t_max * 1e6 / s.ops
        ax.errorbar(s.n, y, yerr=[y - lo, hi - y], marker="o", color=INPUT_COLOR[inp], lw=1.8, ms=5,
                    capsize=3, label=INPUT_LABEL[inp].split(" (")[0])
    ax.set_xscale("log"); ax.xaxis.set_major_formatter(FuncFormatter(plain))
    ax.set_xticks([250, 500, 1000, 2000, 4000]); ax.minorticks_off()
    ax.set_title(title, fontsize=12); ax.set_xlabel("string length n (= m), characters (log scale)")
    ax.set_ylim(0, None)
axes[0].set_ylabel("time per computed cell, ns (median; bars = min–max of 5 runs)")
axes[0].legend(fontsize=9, loc="upper right")
fig.suptitle("DP versions: cost per cell stays constant, so time ∝ n·m", fontsize=13, fontweight="bold")
pc = large.assign(ns=large.time_ms * 1e6 / large.ops)
ratio = (pc[pc.input == "disjoint"].ns.mean() / pc[pc.input == "identical"].ns.mean())
save(fig, "ed_dp_time_per_cell.png",
     f"Time per cell is flat in n (so time ∝ n·m holds), but the disjoint input costs about {ratio:.1f}× more per cell "
     f"than identical strings, because every cell takes the slower 3-way-min branch even though the cell counts are equal.")

# ================================================================== P4 memory
fig, ax = plt.subplots(figsize=(9, 5.4))
mem = large[large.input == "disjoint"]
for alg, col, mk, lab in [("table", C["orange"], "s", "full table (B): (n+1)(m+1) cells"),
                          ("two_rows", C["blue"], "^", "two rows (C): 2(m+1) cells")]:
    s = mem[mem.algorithm == alg].sort_values("n")
    ax.plot(s.n, s.mem_cells, marker=mk, color=col, lw=2, ms=7, label=lab)
t = mem[(mem.algorithm == "table") & (mem.n == 4000)].mem_cells.iloc[0]
w = mem[(mem.algorithm == "two_rows") & (mem.n == 4000)].mem_cells.iloc[0]
ax.annotate(f"{t / 1e6:.0f} M cells ≈ {t * 4 / 1e6:.0f} MB", (4000, t), textcoords="offset points", xytext=(0, 10), ha="right", fontsize=10, fontweight="bold")
ax.annotate(f"{w:,} cells ≈ {w * 4 / 1e3:.0f} KB", (4000, w), textcoords="offset points", xytext=(0, 10), ha="right", fontsize=10, fontweight="bold")
ax.set_ylim(top=1e8)
ax.set_xscale("log"); ax.set_yscale("log")
ax.set_xticks([250, 500, 1000, 2000, 4000]); ax.xaxis.set_major_formatter(FuncFormatter(plain)); ax.minorticks_off()
ax.set_xlabel("string length n (= m), characters (log scale)")
ax.set_ylabel("allocated int cells for DP storage (count, log scale)")
ax.set_title("Memory: quadratic table vs linear two rows")
ax.legend(loc="upper left")
save(fig, "ed_memory.png",
     f"At n=4000 two rows use {t / w:,.0f}× fewer cells than the full table (a straight line of slope 1 vs slope 2 "
     f"on the log-log plot).")

fig, axes = plt.subplots(2, 2, figsize=(10.5, 7), sharex=True, sharey=True)
order = ["worst_aab", "random", "best_nofirst", "log_lines"]
for ax, inp in zip(axes.flat, order):
    for alg, col, mk in [("naive", C["red"], "o"), ("kmp", C["blue"], "s")]:
        s = ssize[(ssize.input == inp) & (ssize.algorithm == alg)].sort_values("n")
        ax.plot(s.n, s.ops / s.n, marker=mk, color=col, lw=2, ms=5, label=alg.upper() if alg == "kmp" else "Naive")
        ax.annotate(f"{(s.ops / s.n).iloc[-1]:.2f}", (s.n.iloc[-1], (s.ops / s.n).iloc[-1]),
                    textcoords="offset points", xytext=(-4, 7 if alg == "naive" else -14), ha="right", fontsize=9, color=col)
    ax.set_xscale("log"); ax.set_yscale("log"); ax.set_ylim(0.6, 250)
    ax.set_yticks([1, 2, 5, 10, 20, 50, 100]); ax.yaxis.set_major_formatter(FuncFormatter(lambda v, _: f"{v:g}"))
    ax.set_title(INPUT_LABEL[inp], fontsize=10.5)
    ax.set_xticks([1e5, 4e5, 1.6e6]); ax.xaxis.set_major_formatter(FuncFormatter(plain)); ax.minorticks_off()
axes[0, 0].legend(loc="center left", fontsize=9)
for ax in axes[1]: ax.set_xlabel("text length n, characters (log scale)")
for ax in axes[:, 0]: ax.set_ylabel("comparisons per text char (log scale)")
fig.suptitle("Character comparisons per text character (pattern length m = 100)", fontsize=13, fontweight="bold", y=0.99)
fig.tight_layout(rect=(0, 0, 1, 0.97))
save(fig, "search_comparisons_vs_n.png",
     "Naive does ~100 comparisons per character on the worst input and exactly 1 on the best, KMP stays "
     "between 1 and 2 on every input, and neither depends on n (flat lines = linear time).")

fig, axes = plt.subplots(1, 2, figsize=(11, 5.2), sharey=True)
for ax, alg, title in [(axes[0], "naive", "Naive"), (axes[1], "kmp", "KMP")]:
    for inp in ["worst_aab", "random", "log_lines", "best_nofirst"]:
        s = ssize[(ssize.algorithm == alg) & (ssize.input == inp)].sort_values("n")
        y = s.time_ms
        ax.errorbar(s.n, y, yerr=[y - s.t_min, s.t_max - y], marker="o", color=INPUT_COLOR[inp], lw=1.8, ms=5,
                    capsize=3, label=INPUT_LABEL[inp])
    n0 = np.array([1e5, 3.2e6])
    ax.plot(n0, 0.1 * n0 / 1e5 * 1.0, ":", color=C["grey"], lw=1.2, label="reference: time ∝ n")
    ax.set_xscale("log"); ax.set_yscale("log")
    ax.set_xticks([1e5, 4e5, 1.6e6]); ax.xaxis.set_major_formatter(FuncFormatter(plain)); ax.minorticks_off()
    ax.yaxis.set_major_formatter(FuncFormatter(lambda v, _: f"{v:g}"))
    ax.set_title(title, fontsize=12); ax.set_xlabel("text length n, characters (log scale)")
axes[0].set_ylabel("median time per search, ms (log scale; bars = min–max of 5 runs)")
axes[0].legend(fontsize=8.5, loc="upper left")
fig.suptitle("Search time vs text length (pattern length m = 100)", fontsize=13, fontweight="bold")
a = ssize[(ssize.algorithm == "naive") & (ssize.input == "worst_aab") & (ssize.n == 3_200_000)].time_ms.iloc[0]
b = ssize[(ssize.algorithm == "kmp") & (ssize.input == "worst_aab") & (ssize.n == 3_200_000)].time_ms.iloc[0]
save(fig, "search_time_vs_n.png",
     f"Naive on its worst input is ≈{a / b:.0f}× slower than KMP on the same text ({a:.0f} ms vs {b:.0f} ms at "
     f"n=3.2M); every other line grows roughly like the linear reference, with visible timing noise on runs shorter than ~1 ms.")

fig, ax = plt.subplots(figsize=(9, 5.4))
for alg, col, mk, lab in [("naive", C["red"], "o", "Naive"), ("kmp", C["blue"], "s", "KMP")]:
    s = spat[spat.algorithm == alg].sort_values("m")
    ax.plot(s.m, s.ops / s.n, marker=mk, color=col, lw=2, ms=7, label=lab)
mm = np.array([10, 400])
ax.plot(mm, mm, ":", color=C["grey"], lw=1.3, label="reference: y = m")
ax.set_xscale("log"); ax.set_yscale("log")
ax.set_xticks([10, 25, 50, 100, 200, 400]); ax.xaxis.set_major_formatter(FuncFormatter(plain)); ax.minorticks_off()
ax.yaxis.set_major_formatter(FuncFormatter(lambda v, _: f"{v:g}"))
ax.set_xlabel("pattern length m, characters (log scale)")
ax.set_ylabel("comparisons per text char (log scale)")
ax.set_title("Worst-case input, n = 1,000,000: cost vs pattern length")
ax.legend(loc="upper left")
sn = spat[(spat.algorithm == "naive") & (spat.m == 400)]; sk = spat[(spat.algorithm == "kmp") & (spat.m == 400)]
save(fig, "search_pattern_length.png",
     f"Naive's cost per character grows in step with m ({(sn.ops / sn.n).iloc[0]:.0f} at m=400) while KMP stays "
     f"at {(sk.ops / sk.n).iloc[0]:.1f}, so the longer the pattern, the more KMP wins.")

fig, ax = plt.subplots(figsize=(9, 5.4))
big = ssize[ssize.n == 3_200_000]
names = ["worst_aab", "random", "log_lines", "best_nofirst"]
tr, cr = [], []
for inp in names:
    nv = big[(big.input == inp) & (big.algorithm == "naive")].iloc[0]
    kv = big[(big.input == inp) & (big.algorithm == "kmp")].iloc[0]
    tr.append(nv.time_ms / kv.time_ms); cr.append(nv.ops / kv.ops)
x = np.arange(len(names)); w = 0.38
b1 = ax.bar(x - w / 2, cr, w, color=C["orange"], label="comparisons ratio (counter)")
b2 = ax.bar(x + w / 2, tr, w, color=C["blue"], label="time ratio (clock, median of 5)")
for bars in (b1, b2):
    for r_ in bars:
        ax.text(r_.get_x() + r_.get_width() / 2, r_.get_height() * 1.08, f"{r_.get_height():.2f}×" if r_.get_height() < 10 else f"{r_.get_height():.0f}×",
                ha="center", fontsize=9)
ax.axhline(1, color=C["black"], lw=1)
ax.set_yscale("log"); ax.set_ylim(0.3, 150)
ax.yaxis.set_major_formatter(FuncFormatter(lambda v, _: f"{v:g}×"))
ax.set_xticks(x); ax.set_xticklabels(["worst case\naⁿ / aᵐ⁻¹b", "random", "server log", "best case\naⁿ / bᵐ"])
ax.set_ylabel("Naive ÷ KMP, ratio (log scale; 1× = equal)")
ax.set_xlabel("input type (n = 3,200,000, m = 100; log pattern m = 31)")
ax.set_title("When is KMP actually worth it? Naive ÷ KMP at the largest n")
ax.legend(loc="upper right", fontsize=9)
save(fig, "search_naive_vs_kmp_ratio.png",
     f"KMP wins big only on the adversarial input ({tr[0]:.0f}× faster); on random text and on a realistic log file "
     f"the comparison counts differ by only {cr[1]:.2f}× and {cr[2]:.2f}×, and the clock ratio stays within {max(tr[1:3]):.1f}×.")
print("summary rows:", len(summary))
