from __future__ import annotations

import argparse
from dataclasses import dataclass
from datetime import UTC, datetime
from pathlib import Path

import requests
from bs4 import BeautifulSoup

from source_catalog import SOURCE_RECORDS, STORY_IDS


USER_AGENT = "LittleMandarinClassicsContentPipeline/0.1"


@dataclass(frozen=True)
class FetchResult:
    url: str
    ok: bool
    status_code: int | None
    title: str
    error: str


def fetch_source_title(url: str) -> FetchResult:
    try:
        response = requests.get(url, headers={"User-Agent": USER_AGENT}, timeout=20)
        response.raise_for_status()
    except requests.RequestException as exc:
        status = exc.response.status_code if exc.response is not None else None
        return FetchResult(url=url, ok=False, status_code=status, title="", error=str(exc))

    soup = BeautifulSoup(response.text, "html.parser")
    heading = soup.find(id="firstHeading")
    title = heading.get_text(" ", strip=True) if heading else soup.title.get_text(" ", strip=True)
    return FetchResult(url=url, ok=True, status_code=response.status_code, title=title, error="")


def render_source_md(story_id: str, fetches: list[FetchResult]) -> str:
    record = SOURCE_RECORDS[story_id]
    generated = datetime.now(UTC).date().isoformat()
    lines = [
        f"# {record['title']}",
        "",
        f"- story_id: {story_id}",
        f"- source_title: {record['source_title']}",
        f"- source_url: {record['source_url']}",
        "- related_urls:",
    ]
    lines.extend(f"  - {url}" for url in record["related_urls"])
    lines.extend(
        [
            f"- fetched_at_utc: {generated}",
            f"- license: {record['license']}",
            "",
            "## Fetch Check",
        ]
    )
    for item in fetches:
        status = "ok" if item.ok else "failed"
        detail = item.title if item.ok else item.error
        lines.append(f"- {item.url}: {status} ({detail})")

    lines.extend(
        [
            "",
            "## 中文梗概",
            record["summary"],
            "",
            "## 改写取舍",
            record["rewrite_focus"],
            "",
            "## 原文相关片段",
        ]
    )
    lines.extend(f"> {excerpt}" for excerpt in record["excerpts"])
    lines.append("")
    return "\n".join(lines)


def scrape_story(story_id: str, output_dir: Path, fetch: bool = True) -> Path:
    record = SOURCE_RECORDS[story_id]
    fetches = [fetch_source_title(url) for url in record["related_urls"]] if fetch else []
    story_source_dir = output_dir / story_id
    story_source_dir.mkdir(parents=True, exist_ok=True)
    source_path = story_source_dir / "source.md"
    source_path.write_text(render_source_md(story_id, fetches), encoding="utf-8")
    return source_path


def scrape_all(output_dir: Path, ids: list[str] | None = None, fetch: bool = True) -> list[Path]:
    selected = ids or STORY_IDS
    return [scrape_story(story_id, output_dir, fetch=fetch) for story_id in selected]


def main() -> int:
    parser = argparse.ArgumentParser(description="Fetch and record public-domain source notes.")
    parser.add_argument("--output-dir", type=Path, default=Path("content/sources"))
    parser.add_argument("--ids", nargs="*", default=None, help="Optional story ids to scrape.")
    parser.add_argument("--no-fetch", action="store_true", help="Write source files without URL fetch checks.")
    args = parser.parse_args()

    paths = scrape_all(args.output_dir, ids=args.ids, fetch=not args.no_fetch)
    for path in paths:
        print(path)
    print(f"Wrote {len(paths)} source files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
