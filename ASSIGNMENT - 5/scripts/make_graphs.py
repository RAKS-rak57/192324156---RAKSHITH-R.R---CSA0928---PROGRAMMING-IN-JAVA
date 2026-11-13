import csv, os, re
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt, numpy as np
def read(p): return list(csv.DictReader(open(p)))
os.makedirs("graphs", exist_ok=True)
def save(n): plt.tight_layout(); plt.savefig("graphs/" + n, dpi=130); plt.close()

# ---- B1.3 buffering
r = read("TaskB1/buffering_results.csv"); methods = []
for x in r:
    if x["method"] not in methods: methods.append(x["method"])
fig, ax = plt.subplots(1, 2, figsize=(11, 4.2))
for m in methods:
    pts = [(int(x["size_MB"]), float(x["time_ms"])) for x in r if x["method"] == m]
    ax[0].plot([p[0] for p in pts], [p[1] for p in pts], "o-", label=m)
ax[0].set_xscale("log"); ax[0].set_yscale("log"); ax[0].set_xlabel("file size (MB)"); ax[0].set_ylabel("time to count the lines (ms)"); ax[0].set_title("Time"); ax[0].legend(fontsize=8); ax[0].grid(True, which="both", alpha=.3)
calls = [(m, int([x for x in r if x["method"] == m and x["size_MB"] == "50"][0]["read_calls"])) for m in methods[:3]]
ax[1].bar([c[0] for c in calls], [c[1] for c in calls], color=["#C0504D", "#F79646", "#9BBB59"]); ax[1].set_yscale("log"); ax[1].set_ylabel("read calls reaching the file stream (50 MB)"); ax[1].set_title("Read calls (log scale)")
for i, c in enumerate(calls): ax[1].text(i, c[1], f"{c[1]:,}", ha="center", va="bottom", fontsize=8)
plt.setp(ax[1].get_xticklabels(), fontsize=8); save("b1_buffering.png")

# ---- B1.4 growth
r = read("TaskB1/growth_results.csv"); n = [int(x["n"]) for x in r]
plt.figure(figsize=(7, 4.4))
plt.plot(n, [float(x["scan_last_ms"]) for x in r], "o-", label="text file: find last pass (scan)")
plt.plot(n, [float(x["scan_missing_ms"]) for x in r], "s-", label="text file: id not present (scan)")
plt.plot(n, [float(x["fixed_seek_us"]) / 1000 for x in r], "^-", label="fixed-length records: seek")
plt.plot(n, [float(x["map_lookup_ns"]) / 1e6 for x in r], "d-", label="HashMap lookup (after loading once)")
plt.xscale("log"); plt.yscale("log"); plt.xlabel("number of passes n"); plt.ylabel("time for ONE find (ms)"); plt.title("B1.4 Growth of the time to find a pass"); plt.legend(fontsize=8); plt.grid(True, which="both", alpha=.3); save("b1_growth.png")
r = read("TaskB1/search_many_results.csv"); n = [int(x["n"]) for x in r]
plt.figure(figsize=(6.4, 4.2)); x = np.arange(len(n)); w = .35
plt.bar(x - w / 2, [float(a["scan_total_ms"]) for a in r], w, label="scan the file for every search"); plt.bar(x + w / 2, [float(a["hashmap_total_ms"]) for a in r], w, label="HashMap: load once + lookups")
plt.yscale("log"); plt.xticks(x, [f"n={a:,}" for a in n]); plt.ylabel("total time for q = 100 searches (ms)"); plt.title("q searches on n passes"); plt.legend(fontsize=8); plt.grid(True, axis="y", alpha=.3); save("b1_search_many.png")

# ---- A4 formats
r = [x for x in read("TaskA4/format_results.csv") if x["n"] == "100000"]
fig, ax = plt.subplots(1, 3, figsize=(11, 3.8))
for a, key, ttl in [(ax[0], "file_bytes", "file size (bytes)"), (ax[1], "save_ms", "save time (ms)"), (ax[2], "load_ms", "load time (ms)")]:
    a.bar([x["format"] for x in r], [float(x[key]) for x in r], color=["#4F81BD", "#9BBB59", "#F79646"]); a.set_title(ttl + ", n = 100,000"); plt.setp(a.get_xticklabels(), fontsize=8)
save("a4_formats.png")

# ---- B3 loader
r = read("TaskB3/loader_results.csv")
fig, ax = plt.subplots(1, 2, figsize=(11.5, 4.4))
for a, d, ttl in [(ax[0], "/tmp", "database on disk (every commit is made durable)"), (ax[1], "/dev/shm", "database in memory (no disk wait)")]:
    sizes = [1000, 10000, 100000]; combos = [("Statement", "autocommit"), ("PreparedStatement", "autocommit"), ("Prepared+Batch(1000)", "autocommit"), ("Statement", "transaction"), ("PreparedStatement", "transaction"), ("Prepared+Batch(1000)", "transaction")]
    colors = ["#C0504D", "#E6A09E", "#F2D0CF", "#4F81BD", "#8EB4E3", "#C6D9F1"]
    for i, (ap, mo) in enumerate(combos):
        vals = []
        for s in sizes:
            rows = [float(x["ms"]) for x in r if x["dir"] == d and int(x["rows"]) == s and x["approach"] == ap and x["mode"] == mo]
            vals.append(min(rows) if rows else np.nan)
        a.bar(np.arange(3) + (i - 2.5) * 0.13, vals, 0.13, color=colors[i], label=f"{ap}, {'auto-commit' if mo == 'autocommit' else 'one transaction'}")
    a.set_yscale("log"); a.set_xticks(range(3)); a.set_xticklabels(["1,000", "10,000", "100,000"]); a.set_xlabel("rows inserted"); a.set_ylabel("total time (ms)"); a.set_title(ttl, fontsize=10); a.grid(True, axis="y", alpha=.3)
ax[0].legend(fontsize=7, loc="upper center", bbox_to_anchor=(1.1, -0.15), ncol=3); save("b3_loader.png")

# ---- B4 scale
r = read("TaskB4/scale_results.csv"); g = lambda d, m: float([x for x in r if x["design"] == d and x["measure"] == m][0]["value"])
labels = ["find ONE pass", "list ONE route\n(20,000 passes)"]
vals = {"text file (scan)": [g("text file", "find one pass"), g("text file", "list one route")],
        "12 serialized files\n(in memory, scan)": [g("per-campus serialization", "search all for one id"), g("per-campus serialization", "list one route")],
        "database (index)": [g("database", "find by primary key"), g("database", "list one route")]}
plt.figure(figsize=(7, 4.4)); x = np.arange(2); w = .26
for i, (k, v) in enumerate(vals.items()):
    b = plt.bar(x + (i - 1) * w, v, w, label=k)
    for j, h in enumerate(v): plt.text(x[j] + (i - 1) * w, h, f"{h:,.3g}", ha="center", va="bottom", fontsize=7)
plt.yscale("log"); plt.xticks(x, labels); plt.ylabel("time (ms), 800,000 passes"); plt.title("B4 Search time at the new scale"); plt.legend(fontsize=8); plt.grid(True, axis="y", alpha=.3); save("b4_scale.png")

# ---- B2 login cost
txt = open("TaskB2/adversarial_output.txt").read()
rows = re.findall(r"^\s{3}(string-built Statement \(unsafe\)|escape quotes \+ Statement|regex validate \+ Statement|PreparedStatement, prepared every time|PreparedStatement, prepared ONCE)\s+([0-9.]+) us", txt, re.M)
plt.figure(figsize=(7.4, 3.8)); plt.barh([a for a, _ in rows][::-1], [float(b) for _, b in rows][::-1], color="#4F81BD"); plt.xlabel("microseconds per login check"); plt.title("B2 Cost of one login check (20,000 logins)")
for i, (a, b) in enumerate(rows[::-1]): plt.text(float(b), i, f" {b}", va="center", fontsize=8)
plt.xlim(0, max(float(b) for _, b in rows) * 1.15); save("b2_login_cost.png")
print("graphs ok")
