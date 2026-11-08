"""Draws all graphs used in the report from the CSV files written by the Java programs.
Run from the repository root:  python3 scripts/make_graphs.py"""
import csv, os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

def read(path):
    with open(path) as f:
        return list(csv.DictReader(f))

def save(name):
    os.makedirs("graphs", exist_ok=True)
    plt.tight_layout()
    plt.savefig("graphs/" + name, dpi=130)
    plt.close()

# ---------- Q1 ----------
rows = read("Q1/q1_scaling.csv")
n = [int(r["fields"]) for r in rows]
plt.figure(figsize=(7, 4.5))
for col, st in [("String +", "o-"), ("StringBuilder", "s-"), ("StringBuffer", "^-"), ("String.join", "d-"), ("StringJoiner", "x-")]:
    plt.plot(n, [float(r[col]) for r in rows], st, label=col)
plt.xscale("log", base=2); plt.yscale("log")
plt.xlabel("number of fields in one ticket (n)"); plt.ylabel("time to build one ticket (ms)")
plt.title("Q1: time vs number of fields (log-log)"); plt.grid(True, which="both", alpha=.3); plt.legend()
save("q1_scaling.png")

rows = read("Q1/q1_workloads.csv")
wl = []
for r in rows:
    if r["workload"] not in wl: wl.append(r["workload"])
apps = ["String +", "StringBuilder", "StringBuffer", "String.join", "StringJoiner"]
plt.figure(figsize=(7.5, 4.5))
w = 0.16
for i, a in enumerate(apps):
    vals = [float([r for r in rows if r["workload"] == x and r["approach"] == a][0]["avg_time_ms"]) for x in wl]
    plt.bar([k + (i - 2) * w for k in range(len(wl))], vals, w, label=a)
plt.xticks(range(len(wl)), wl); plt.yscale("log"); plt.ylabel("average time (ms, log scale)")
plt.title("Q1: three workloads"); plt.legend(); plt.grid(True, axis="y", alpha=.3)
save("q1_workloads.png")

# ---------- Q2 ----------
rows = read("Q2/q2_list_results.csv")
ops = ["add at end", "insert at front", "insert in middle", "remove at front", "remove in middle", "get k-th (random)", "iterate whole list"]
sizes = [10000, 50000, 200000]
fig, axs = plt.subplots(1, 3, figsize=(13, 4.2), sharey=False)
for ax, o in zip(axs, ["insert at front", "insert in middle", "get k-th (random)"]):
    for st, mk in [("ArrayList", "o-"), ("LinkedList", "s-"), ("ArrayDeque", "^-")]:
        pts = [(int(r["size"]), float(r["ns_per_op"]) / 1000) for r in rows if r["operation"] == o and r["structure"] == st]
        if pts: ax.plot([p[0] for p in pts], [p[1] for p in pts], mk, label=st)
    ax.set_xscale("log"); ax.set_yscale("log"); ax.set_title(o); ax.set_xlabel("playlist size N"); ax.grid(True, which="both", alpha=.3)
axs[0].set_ylabel("microseconds per operation"); axs[0].legend()
plt.suptitle("Q2: ArrayList vs LinkedList vs ArrayDeque"); save("q2_operations.png")

rows = read("Q2/q2_upnext_results.csv")
plt.figure(figsize=(6.5, 4.2))
for st, mk in [("ArrayList", "o-"), ("LinkedList", "s-"), ("ArrayDeque", "^-")]:
    pts = [(int(r["size"]), float(r["total_ms_for_100000_mixed_ops"])) for r in rows if r["structure"] == st]
    plt.plot([p[0] for p in pts], [p[1] for p in pts], mk, label=st)
plt.xscale("log"); plt.yscale("log"); plt.xlabel("songs already in queue"); plt.ylabel("time for 100,000 rounds (ms)")
plt.title("Q2: 'Up Next' queue (addLast, addFirst, removeFirst)"); plt.legend(); plt.grid(True, which="both", alpha=.3)
save("q2_upnext.png")

# ---------- Q3 ----------
rows = read("Q3/q3_dedup_results.csv")
n = [int(r["scans"]) for r in rows]
plt.figure(figsize=(6.5, 4.2))
for col, mk in [("HashSet_ms", "o-"), ("TreeSet_ms", "s-"), ("HashMap_ms", "^-")]:
    plt.plot(n, [float(r[col]) for r in rows], mk, label=col.replace("_ms", ""))
plt.xscale("log"); plt.yscale("log"); plt.xlabel("number of scans"); plt.ylabel("time (ms)")
plt.title("Q3: de-duplication of scans"); plt.legend(); plt.grid(True, which="both", alpha=.3)
save("q3_dedup.png")

# ---------- Q4 ----------
rows = read("Q4/q4_map_results.csv")
sizes = sorted({int(r["size"]) for r in rows})
plt.figure(figsize=(7, 4.4))
for st, mk in [("HashMap", "o-"), ("TreeMap", "s-"), ("LinkedHashMap", "^-"), ("Hashtable", "d-"), ("SortedArray+binarySearch", "x-")]:
    plt.plot(sizes, [float([r for r in rows if r["structure"] == st and int(r["size"]) == s][0]["ns_per_lookup"]) for s in sizes], mk, label=st)
plt.xscale("log"); plt.yscale("log"); plt.xlabel("number of medicines N"); plt.ylabel("nanoseconds per lookup")
plt.title("Q4: lookup cost"); plt.legend(); plt.grid(True, which="both", alpha=.3)
save("q4_lookup.png")

fig, axs = plt.subplots(1, 2, figsize=(11, 4.2))
for ax, col, ttl in [(axs[0], "code_order_report_ms", "Report in CODE order"), (axs[1], "added_order_report_ms", "Report in ADDED order")]:
    for st, mk in [("HashMap", "o-"), ("TreeMap", "s-"), ("LinkedHashMap", "^-")]:
        ax.plot(sizes, [float([r for r in rows if r["structure"] == st and int(r["size"]) == s][0][col]) for s in sizes], mk, label=st)
    ax.set_xscale("log"); ax.set_yscale("log"); ax.set_title(ttl); ax.set_xlabel("N"); ax.grid(True, which="both", alpha=.3)
axs[0].set_ylabel("ms"); axs[0].legend()
save("q4_reports.png")

# ---------- Q5 ----------
rows = read("Q5/q5_sort_results.csv")
n = [int(r["n"]) for r in rows]
plt.figure(figsize=(6.8, 4.2))
w = 0.26
for i, (col, lab) in enumerate([("comparator_chain_1sort_ms", "Comparator chain (1 sort)"), ("comparable_1sort_ms", "Comparable (1 sort)"), ("three_stable_sorts_ms", "3 stable sorts")]):
    plt.bar([k + (i - 1) * w for k in range(len(n))], [float(r[col]) for r in rows], w, label=lab)
plt.xticks(range(len(n)), [f"{x:,}" for x in n]); plt.xlabel("number of teams"); plt.ylabel("time (ms)")
plt.title("Q5: ranking designs"); plt.legend(); plt.grid(True, axis="y", alpha=.3)
save("q5_sorts.png")

# ---------- Q6 ----------
rows = read("Q6/q6_growth_results.csv")
n = [int(r["n"]) for r in rows]
plt.figure(figsize=(6.5, 4.2))
plt.plot(n, [float(r["add_ns_per_op"]) for r in rows], "o-", label="add")
plt.plot(n, [float(r["poll_ns_per_op"]) for r in rows], "s-", label="poll (serve next)")
plt.xscale("log"); plt.xlabel("patients in queue (n)"); plt.ylabel("nanoseconds per operation")
plt.title("Q6: PriorityQueue cost per operation"); plt.legend(); plt.grid(True, which="both", alpha=.3)
save("q6_growth.png")

rows = read("Q6/q6_alternatives_results.csv")
n = sorted({int(r["n"]) for r in rows})
plt.figure(figsize=(6.5, 4.2))
for d, mk in [("PriorityQueue", "o-"), ("Sorted LinkedList", "s-"), ("Five FIFO queues", "^-")]:
    plt.plot(n, [float([r for r in rows if r["design"] == d and int(r["n"]) == x][0]["admit_all_ms"]) for x in n], mk, label=d)
plt.xscale("log"); plt.yscale("log"); plt.xlabel("n patients"); plt.ylabel("time to admit all (ms)")
plt.title("Q6: admit cost of three designs"); plt.legend(); plt.grid(True, which="both", alpha=.3)
save("q6_alternatives.png")

# ---------- Q7 ----------
rows = read("Q7/q7_results.csv")
n = sorted({int(r["n"]) for r in rows})
plt.figure(figsize=(7, 4.4))
for d, mk in [("Original (rebuild PQ)", "o-"), ("Redesign (5 queues)", "s-")]:
    plt.plot(n, [float([r for r in rows if r["design"] == d and int(r["n"]) == x][0]["avg_time_ms"]) for x in n], mk, label=d)
plt.xscale("log"); plt.yscale("log"); plt.xlabel("patients waiting (n)"); plt.ylabel("time to serve all n patients (ms)")
plt.title("Q7: original vs redesigned fair triage"); plt.legend(); plt.grid(True, which="both", alpha=.3)
save("q7_comparison.png")

plt.figure(figsize=(7, 4.4))
for d, mk in [("Original (rebuild PQ)", "o-"), ("Redesign (5 queues)", "s-")]:
    plt.plot(n, [int([r for r in rows if r["design"] == d and int(r["n"]) == x][0]["patients_or_heads_examined"]) for x in n], mk, label=d)
plt.xscale("log"); plt.yscale("log"); plt.xlabel("patients waiting (n)"); plt.ylabel("patients (or queue heads) examined")
plt.title("Q7: operation count"); plt.legend(); plt.grid(True, which="both", alpha=.3)
save("q7_operations.png")
print("graphs done")
