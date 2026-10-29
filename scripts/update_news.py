"""RSS -> JSON statique, sans service tiers ni dépendance Python."""
import concurrent.futures
import hashlib
import html
import json
import re
import shutil
import sys
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timezone, timedelta
from email.utils import parsedate_to_datetime
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]
UTC = timezone.utc


class Text(HTMLParser):
    def __init__(self):
        super().__init__()
        self.parts = []
        self.hidden = 0

    def handle_starttag(self, tag, attrs):
        if tag in ('script', 'style'):
            self.hidden += 1

    def handle_endtag(self, tag):
        if tag in ('script', 'style'):
            self.hidden = max(0, self.hidden - 1)

    def handle_data(self, value):
        if not self.hidden:
            self.parts.append(value)


def clean(value):
    parser = Text()
    parser.feed(value or '')
    return re.sub(r'\s+', ' ', html.unescape(' '.join(parser.parts))).strip()


def web_url(value):
    value = (value or '').strip()
    parsed = urlparse(value)
    return value if parsed.scheme in ('https', 'http') and parsed.hostname and not parsed.username else ''


def parse_date(value):
    try:
        date = parsedate_to_datetime(value)
    except (ValueError, TypeError):
        try:
            date = datetime.fromisoformat(value.replace('Z', '+00:00'))
        except (ValueError, TypeError, AttributeError):
            return None
    return date.replace(tzinfo=UTC) if date.tzinfo is None else date.astimezone(UTC)


def local(tag):
    return tag.rsplit('}', 1)[-1]


def parse_feed(raw, feed):
    if b'<!DOCTYPE' in raw.upper() or b'<!ENTITY' in raw.upper():
        raise ValueError('Déclarations XML non acceptées')
    root = ET.fromstring(raw)
    result = []
    for node in root.iter():
        if local(node.tag) not in ('item', 'entry'):
            continue
        fields = {}
        for child in node:
            fields.setdefault(local(child.tag), ''.join(child.itertext()))
        title = clean(fields.get('title'))
        link = web_url(fields.get('link'))
        if not link:
            link = next((web_url(e.get('href')) for e in node if local(e.tag) == 'link' and e.get('rel', 'alternate') == 'alternate'), '')
        if not title or not link:
            continue
        date = parse_date(fields.get('pubDate') or fields.get('published') or fields.get('updated') or fields.get('date'))
        image = ''
        for child in node.iter():
            name = local(child.tag)
            media = name in ('thumbnail', 'content') and ('search.yahoo.com/mrss' in child.tag) and child.get('medium', 'image') == 'image'
            if media or (name == 'enclosure' and child.get('type', '').startswith('image/')):
                image = web_url(child.get('url'))
                if image:
                    break
        result.append({
            'id': hashlib.sha256(link.encode()).hexdigest()[:20],
            'feedId': feed['id'], 'source': feed['name'], 'category': feed['category'],
            'title': title, 'summary': clean(fields.get('description') or fields.get('summary'))[:450],
            'url': link, 'image': image, 'published': date.isoformat() if date else None,
        })
    if not result:
        raise ValueError('Aucun article exploitable dans le flux')
    return result


def fetch(feed):
    request = urllib.request.Request(feed['url'], headers={'User-Agent': 'FilInfo-RSS/1.0', 'Accept': 'application/rss+xml, application/atom+xml, application/xml, text/xml'})
    with urllib.request.urlopen(request, timeout=25) as response:
        raw = response.read(4_000_001)
    if len(raw) > 4_000_000:
        raise ValueError('Flux trop volumineux')
    return parse_feed(raw, feed)


def main():
    config = json.loads((ROOT / 'config.json').read_text())
    output = ROOT / 'public'
    output.mkdir(exist_ok=True)
    for filename in ('index.html', 'styles.css', 'app.js', 'config.json'):
        shutil.copyfile(ROOT / filename, output / filename)
    cache_path = ROOT / '.feed-cache' / 'news.json'
    previous = json.loads(cache_path.read_text()) if cache_path.exists() else {'articles': [], 'sources': []}
    now = datetime.now(UTC)
    cutoff = now - timedelta(hours=config['maxAgeHours'])
    buckets, status = [], []
    previous_status = {s['id']: s for s in previous.get('sources', [])}
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        futures = {feed['id']: pool.submit(fetch, feed) for feed in config['feeds']}
        for feed in config['feeds']:
            try:
                articles = futures[feed['id']].result()
                last_success = now.isoformat()
                state = 'ok'
            except Exception as exc:
                print(f"::warning::{feed['name']} : {type(exc).__name__}. Conservation du cache si disponible.")
                articles = [a for a in previous['articles'] if a['feedId'] == feed['id']]
                last_success = previous_status.get(feed['id'], {}).get('lastSuccess')
                state = 'unavailable'
            # Articles sans date conservés seulement après une collecte récente.
            collected = parse_date(last_success)
            articles = [a for a in articles if (parse_date(a['published']) or collected or cutoff) > cutoff and (parse_date(a['published']) or now) <= now + timedelta(minutes=10)]
            articles.sort(key=lambda a: a['published'] or '', reverse=True)
            buckets.append(articles[:12])
            status.append({'id': feed['id'], 'name': feed['name'], 'status': state, 'lastSuccess': last_success, 'count': len(articles[:12])})
    articles, seen = [], set()
    for i in range(12):
        for bucket in buckets:
            if i < len(bucket) and bucket[i]['url'] not in seen:
                seen.add(bucket[i]['url'])
                articles.append(bucket[i])
    payload = {'generatedAt': now.isoformat(), 'articles': articles, 'sources': status}
    (output / 'news.json').write_text(json.dumps(payload, ensure_ascii=False))
    cache_path.parent.mkdir(exist_ok=True)
    cache_path.write_text(json.dumps(payload, ensure_ascii=False))
    print(f"{len(articles)} articles ; {sum(s['status'] == 'ok' for s in status)}/4 flux récupérés.")
    if not articles:
        print('Aucun article disponible : la page affichera un état explicite, sans faux titres.')


if __name__ == '__main__':
    main()
