import csv, os
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt
def read(p): return list(csv.DictReader(open(p)))
os.makedirs("graphs", exist_ok=True)
def save(n): plt.tight_layout(); plt.savefig("graphs/"+n, dpi=130); plt.close()

r = read("Task01/dispatch_results.csv"); k=[int(x["k"]) for x in r]
plt.figure(figsize=(6.6,4.2))
for col,lab,m in [("chain_ns_per_event","if-else chain (O(k))","o-"),("map_ns_per_event","HashMap (O(1))","s-"),("own_listener_ns_per_event","own listener (O(1), inlined)","^-")]:
    plt.plot(k,[max(float(x[col]),0.05) for x in r],m,label=lab)
plt.xscale("log"); plt.yscale("log"); plt.xlabel("number of buttons k"); plt.ylabel("ns per event (last button)"); plt.title("Task 1: dispatch time"); plt.legend(); plt.grid(True,which="both",alpha=.3); save("task1_dispatch.png")

r = read("Task02/cache_results.csv"); lab=[f"d={x['departments']}\nm={int(x['doctors']):,}" for x in r]
plt.figure(figsize=(6.6,4.2)); import numpy as np; x=np.arange(len(r)); w=.35
plt.bar(x-w/2,[float(a["cache_ns_per_change"]) for a in r],w,label="Map lookup"); plt.bar(x+w/2,[float(a["db_scan_ns_per_change"]) for a in r],w,label="query (scan, no network)")
plt.yscale("log"); plt.xticks(x,lab); plt.ylabel("ns per department change"); plt.title("Task 2: cache vs query"); plt.legend(); plt.grid(True,axis="y",alpha=.3); save("task2_cache.png")

r = read("Task04/repaint_results.csv"); n=[int(a["n"]) for a in r]
plt.figure(figsize=(6.6,4.2)); plt.plot(n,[float(a["points_repaint_us"]) for a in r],"o-",label="redraw n points"); plt.plot(n,[float(a["image_repaint_us"]) for a in r],"s-",label="copy one image")
plt.xscale("log"); plt.yscale("log"); plt.xlabel("number of clicks n"); plt.ylabel("microseconds per repaint"); plt.title("Task 4: repaint cost"); plt.legend(); plt.grid(True,which="both",alpha=.3); save("task4_repaint.png")

r = read("Task06/pagination_results.csv"); ns=sorted({int(a["rows"]) for a in r})
plt.figure(figsize=(6.6,4.2))
for ap,m in [("all at once","o-"),("page of 50","s-")]:
    plt.plot(ns,[float([a for a in r if int(a["rows"])==q and a["approach"]==ap][0]["edt_model_fill_ms"]) for q in ns],m,label=ap)
plt.xscale("log"); plt.yscale("log"); plt.xlabel("registrations N"); plt.ylabel("event thread blocked while filling the table (ms)"); plt.title("Task 6: all rows vs one page"); plt.legend(); plt.grid(True,which="both",alpha=.3); save("task6_pagination.png")
print("graphs ok")
