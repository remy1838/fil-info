"""Refresh official landscape GTA VI artwork; keep the last valid page on errors."""
import html
import json
import re
import sys
import urllib.request
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

GALLERY = "https://www.rockstargames.com/VI/media/artwork-wallpapers"
BASE = "https://www.rockstargames.com/VI/_next/static/media/"
ROOT = Path(__file__).resolve().parents[1]
PAGE = ROOT / "gta6/index.html"
STATE = ROOT / "gta6/artwork-state.json"

def fetch_names():
    req = urllib.request.Request(GALLERY, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=25) as response:
        source = html.unescape(response.read().decode("utf-8"))
    found = list(dict.fromkeys(re.findall(r"([A-Za-z0-9_~-]+_landscape\.[A-Za-z0-9_.~-]+\.(?:jpg|webp|png))", source)))
    names = [name for name in found if not re.search(r"With_?Logos?", name, re.I)]
    if len(names) < 6:
        raise ValueError("Official gallery did not expose enough landscape artwork")
    return names[:12]

def update():
    page = PAGE.read_text()
    old = json.loads(STATE.read_text()) if STATE.exists() else {}
    try:
        with urllib.request.urlopen("https://remy1838.github.io/fil-info/gta6/artwork-state.json", timeout=10) as previous:
            live = json.load(previous)
            if len(live.get("pool", [])) >= 3:
                old = live
    except Exception:
        pass
    try:
        names = fetch_names()
    except Exception as exc:
        names = old.get("pool", [])
        if len(names) < 3:
            print("Artwork refresh skipped: " + str(exc))
            return
        print("Official gallery unavailable; rotating the last verified set.")
    today = datetime.now(ZoneInfo("Europe/Paris")).date()
    signature = "|".join(names)
    previous_signature = "|".join(old.get("pool", []))
    anchor = old.get("anchor_day", today.toordinal())
    if signature != previous_signature:
        anchor = today.toordinal()
    start = ((today.toordinal() - anchor) * 3) % len(names)
    selected = [names[(start + offset) % len(names)] for offset in range(3)]
    replacement = "const artworks=[\n" + ",\n".join("'" + name + "'" for name in selected) + "\n];"
    page, count = re.subn(r"const artworks=\[[\s\S]*?\];", lambda _: replacement, page, count=1)
    if count != 1:
        raise ValueError("Countdown image array was not found; leaving page untouched")
    page = re.sub(r"\nconst latestArtworks=\[[\s\S]*?\];", "", page)
    page = page.replace("const parisHour=", "const latestArtworks=" + json.dumps(names) + ";\nconst parisHour=", 1)
    page = re.sub(r'(<img class="art"[^>]*src=")[^"]+', lambda m: m.group(1) + BASE + selected[0] + "?akim=1&amp;imdensity=1&amp;imwidth=1920", page, count=1)
    PAGE.write_text(page)
    # The published HTML remains the app's existing synchronization source.
    STATE.write_text(json.dumps({"pool": names, "anchor_day": anchor, "selected": selected, "checked_paris": datetime.now(ZoneInfo("Europe/Paris")).isoformat()}, indent=2) + "\n")
    print(json.dumps({"pool_count": len(names), "today": selected}))

if __name__ == "__main__":
    update()
