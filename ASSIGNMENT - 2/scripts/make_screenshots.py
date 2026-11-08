"""Renders console output text files as terminal-style PNG images (output screenshots).
Run from the repository root:  python3 scripts/make_screenshots.py"""
import os
from PIL import Image, ImageDraw, ImageFont

FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"
JOBS = [
    ("Q1/output.txt", "Q1/screenshot_TicketBuilder.png", "java TicketBuilder"),
    ("Q2/output.txt", "Q2/screenshot_PlaylistExperiments.png", "java PlaylistExperiments"),
    ("Q3/attendance_before_output.txt", "Q3/screenshot_AttendanceBefore.png", "java AttendanceBefore"),
    ("Q3/attendance_after_output.txt", "Q3/screenshot_Attendance.png", "java Attendance"),
    ("Q3/output.txt", "Q3/screenshot_HashContractExperiments.png", "java HashContractExperiments"),
    ("Q4/output.txt", "Q4/screenshot_PharmacyInventory.png", "java PharmacyInventory"),
    ("Q5/leaderboard_output.txt", "Q5/screenshot_LeaderboardDemo.png", "java LeaderboardDemo"),
    ("Q5/output.txt", "Q5/screenshot_DesignTests.png", "java DesignTests"),
    ("Q6/triage_output.txt", "Q6/screenshot_Triage.png", "java Triage"),
    ("Q6/output.txt", "Q6/screenshot_TriageExperiments.png", "java TriageExperiments"),
    ("Q7/output.txt", "Q7/screenshot_TriageFair.png", "java TriageFair"),
]
font = ImageFont.truetype(FONT, 15)
bold = ImageFont.truetype(FONT, 15)
for src, dst, cmd in JOBS:
    lines = open(src).read().rstrip("\n").split("\n")
    lines = ["$ " + cmd] + lines
    # wrap very long lines
    wrapped = []
    for l in lines:
        while len(l) > 150:
            wrapped.append(l[:150]); l = "    " + l[150:]
        wrapped.append(l)
    w = max(len(l) for l in wrapped) * 9 + 40
    h = len(wrapped) * 21 + 60
    img = Image.new("RGB", (max(w, 600), h), (30, 30, 30))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, img.width, 30], fill=(60, 60, 60))
    for i, c in enumerate([(255, 95, 86), (255, 189, 46), (39, 201, 63)]):
        d.ellipse([12 + i * 22, 9, 24 + i * 22, 21], fill=c)
    d.text((90, 7), "Terminal - " + os.path.dirname(src), fill=(200, 200, 200), font=font)
    y = 42
    for l in wrapped:
        d.text((16, y), l, fill=(120, 230, 120) if l.startswith("$ ") else (230, 230, 230), font=font)
        y += 21
    img.save(dst)
print("screenshots done")
