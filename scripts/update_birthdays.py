"""Read the shared birthday sheet; publish only today's names, never birth dates."""
import csv, io, json, re
from datetime import datetime, date
from pathlib import Path
from urllib.request import urlopen, Request
from zoneinfo import ZoneInfo

URL = "https://docs.google.com/spreadsheets/d/1nzY39q6GzWgCNj2cx2KwxpBIGjjy60cJae8UfNcQgR8/export?format=csv&gid=0"

def day_month(value):
    s = value.strip()
    m = re.fullmatch(r"(\d{1,2})[/.\-](\d{1,2})(?:[/.\-](\d{4}))?", s)
    if m:
        d, mo, year = int(m[1]), int(m[2]), int(m[3] or 2000)
    else:
        m = re.fullmatch(r"(\d{4})-(\d{2})-(\d{2})", s)
        if not m:
            raise ValueError("Invalid birthday format")
        year, mo, d = map(int, m.groups())
    date(year, mo, d)
    return d, mo

def select(rows, today):
    people, invalid = [], 0
    headers = next(rows, None)
    if headers is None or [h.strip().lower() for h in headers[:3]] != ["nom", "prénom", "date de naissance"]:
        raise ValueError("Expected columns: Nom, Prénom, Date de naissance")
    seen = set()
    for row in rows:
        if not any(c.strip() for c in row):
            continue
        if len(row) < 3 or not row[0].strip() or not row[1].strip():
            invalid += 1
            continue
        try:
            dm = day_month(row[2])
        except ValueError:
            invalid += 1
            continue
        if dm != (today.day, today.month):
            continue
        name = (row[1].strip()[:80], row[0].strip()[:80])
        if name not in seen:
            people.append(dict(prenom=name[0], nom=name[1]))
            seen.add(name)
    return people, invalid

def main():
    now = datetime.now(ZoneInfo("Europe/Paris"))
    payload = dict(date=now.date().isoformat(), updatedAt=now.isoformat(), people=[], status="error")
    try:
        with urlopen(Request(URL, headers={"User-Agent": "Heyrieux-Birthdays/1.0"}), timeout=25) as response:
            if "text/csv" not in response.headers.get("Content-Type", ""):
                raise ValueError("Expected CSV")
            raw = response.read(2000001)
            if len(raw) > 2000000:
                raise ValueError("Sheet too large")
        people, invalid = select(csv.reader(io.StringIO(raw.decode("utf-8-sig"))), now.date())
        payload.update(people=people, status="ok")
        print(f"Birthday data ready: {len(people)} today; {invalid} invalid row(s).")
    except Exception:
        print("Birthday sheet unavailable or invalid; screen will fall back to news.")
    target = Path("public/anniversaire-heyrieux/anniversaires.json")
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")

if __name__ == "__main__":
    main()
