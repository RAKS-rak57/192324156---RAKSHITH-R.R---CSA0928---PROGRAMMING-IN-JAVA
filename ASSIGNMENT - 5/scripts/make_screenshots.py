import os, glob
from PIL import Image, ImageDraw, ImageFont
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"
font = ImageFont.truetype(FONT, 14)
JOBS = [("TaskB1/passstore_output.txt", "TaskB1", "java PassStore"), ("TaskB1/buspassstore_test_output.txt", "TaskB1", "java BusPassStoreTest"),
 ("TaskB1/buffering_output.txt", "TaskB1", "java BufferingBenchmark"), ("TaskB1/growth_output.txt", "TaskB1", "java GrowthBenchmark"),
 ("TaskA3/a3_output.txt", "TaskA3", "java A3Demo"), ("TaskA4/serialization_output.txt", "TaskA4", "java SerializationDemo"), ("TaskA4/versioning_output.txt", "TaskA4", "./versioning.sh"),
 ("TaskA4/format_output.txt", "TaskA4", "java FormatBenchmark"), ("TaskA5/transfer_output.txt", "TaskA5", "java TransferDemo"),
 ("TaskB2/adversarial_output.txt", "TaskB2", "java AdversarialDemo"), ("TaskB2/damagedfile_output.txt", "TaskB2", "java DamagedFileDemo"),
 ("TaskB3/b3_correctness_output.txt", "TaskB3", "java B3Correctness"), ("TaskB3/failure_output.txt", "TaskB3", "java FailureDemo"), ("TaskB3/multirow_output.txt", "TaskB3", "java MultiRowBenchmark"),
 ("TaskB4/migration_output.txt", "TaskB4", "java MigrationDemo"), ("TaskB4/scale_output.txt", "TaskB4", "java ScaleBenchmark"), ("TaskB4/concurrency_output.txt", "TaskB4", "java ConcurrencyDemo")]
for src, task, cmd in JOBS:
    lines = ["$ " + cmd] + open(src).read().rstrip("\n").split("\n")
    wrapped = []
    for l in lines:
        while len(l) > 170: wrapped.append(l[:170]); l = "    " + l[170:]
        wrapped.append(l)
    wrapped = wrapped[:80]
    w = max(max(len(l) for l in wrapped) * 8.4 + 40, 640); h = len(wrapped) * 19 + 56
    img = Image.new("RGB", (int(w), int(h)), (30, 30, 30)); d = ImageDraw.Draw(img)
    d.rectangle([0, 0, w, 28], fill=(60, 60, 60))
    for i, c in enumerate([(255, 95, 86), (255, 189, 46), (39, 201, 63)]): d.ellipse([12 + i * 22, 8, 24 + i * 22, 20], fill=c)
    d.text((90, 6), "Terminal - " + task + "   (rendered from the captured console output)", fill=(200, 200, 200), font=font)
    y = 40
    for l in wrapped: d.text((14, y), l, fill=(120, 230, 120) if l.startswith("$ ") else (230, 230, 230), font=font); y += 19
    os.makedirs("screenshots/" + task, exist_ok=True)
    img.save("screenshots/" + task + "/screenshot_" + os.path.basename(src).replace(".txt", ".png"))
print("screenshots ok")
