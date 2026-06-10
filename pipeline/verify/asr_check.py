from __future__ import annotations

import argparse
import json
import sys
import unicodedata
from dataclasses import asdict, dataclass
from datetime import datetime
from pathlib import Path
from typing import Any, Iterable

from pypinyin import Style, pinyin


REPO_ROOT = Path(__file__).resolve().parents[2]
PIPELINE_ROOT = REPO_ROOT / "pipeline"
if str(PIPELINE_ROOT) not in sys.path:
    sys.path.insert(0, str(PIPELINE_ROOT))

from transformer.audio import read_audio_manifest, split_paragraph_to_sentences  # noqa: E402


@dataclass(frozen=True)
class NormalizedText:
    text: str
    chars: list[str]
    pinyins: list[str]
    source_indices: list[int]


@dataclass(frozen=True)
class AlignOp:
    op: str
    expected_index: int | None
    actual_index: int | None
    expected_char: str
    actual_char: str


@dataclass(frozen=True)
class AlignmentResult:
    operations: list[AlignOp]
    expected_len: int
    actual_len: int
    match_count: int
    substitution_count: int
    insertion_count: int
    deletion_count: int
    edit_count: int
    match_rate: float


@dataclass(frozen=True)
class CharMismatch:
    expected_char: str
    expected_pinyin: str
    actual_char: str
    actual_pinyin: str
    kind: str
    label: str
    counts_as_tts_error: bool
    note: str


@dataclass
class SentenceItem:
    story_id: str
    title_zh: str
    audio_path: str
    abs_audio_path: str
    para_index: int
    sent_index: int
    expected_text: str
    expected_norm: NormalizedText
    expected_char_durations_ms: list[int | None]
    manifest_text: str
    manifest_mismatch: bool
    duration_ms: int


@dataclass
class SentenceAssessment:
    story_id: str
    title_zh: str
    audio_path: str
    para_index: int
    sent_index: int
    expected_text: str
    asr_text: str
    asr_no_context_text: str
    expected_norm: str
    asr_norm: str
    match_rate: float
    edit_count: int
    expected_len: int
    actual_len: int
    mismatch_summary: str
    judgment_code: str
    judgment_label: str
    recommendation: str
    manifest_mismatch: bool
    duration_ms: int
    char_issues: list[dict[str, Any]]


def is_punctuation_or_space(char: str) -> bool:
    if char.isspace():
        return True
    category = unicodedata.category(char)
    return category.startswith("P")


def normalize_for_compare(text: str, pinyins: list[str] | None = None) -> NormalizedText:
    chars: list[str] = []
    kept_pinyins: list[str] = []
    source_indices: list[int] = []
    source_pinyins = pinyins or [""] * len(text)

    for index, char in enumerate(text):
        if is_punctuation_or_space(char):
            continue
        chars.append(char)
        kept_pinyins.append(source_pinyins[index] if index < len(source_pinyins) else "")
        source_indices.append(index)

    return NormalizedText(
        text="".join(chars),
        chars=chars,
        pinyins=kept_pinyins,
        source_indices=source_indices,
    )


def align_normalized_text(expected: NormalizedText, actual: NormalizedText) -> AlignmentResult:
    a = expected.chars
    b = actual.chars
    rows = len(a) + 1
    cols = len(b) + 1
    dp = [[0] * cols for _ in range(rows)]

    for i in range(1, rows):
        dp[i][0] = i
    for j in range(1, cols):
        dp[0][j] = j

    for i in range(1, rows):
        for j in range(1, cols):
            if a[i - 1] == b[j - 1]:
                cost = 0
            else:
                cost = 1
            dp[i][j] = min(
                dp[i - 1][j - 1] + cost,
                dp[i - 1][j] + 1,
                dp[i][j - 1] + 1,
            )

    operations: list[AlignOp] = []
    i = len(a)
    j = len(b)
    while i > 0 or j > 0:
        if i > 0 and j > 0:
            cost = 0 if a[i - 1] == b[j - 1] else 1
            if dp[i][j] == dp[i - 1][j - 1] + cost:
                operations.append(
                    AlignOp(
                        op="match" if cost == 0 else "substitute",
                        expected_index=i - 1,
                        actual_index=j - 1,
                        expected_char=a[i - 1],
                        actual_char=b[j - 1],
                    )
                )
                i -= 1
                j -= 1
                continue
        if i > 0 and dp[i][j] == dp[i - 1][j] + 1:
            operations.append(
                AlignOp(
                    op="delete",
                    expected_index=i - 1,
                    actual_index=None,
                    expected_char=a[i - 1],
                    actual_char="",
                )
            )
            i -= 1
            continue
        operations.append(
            AlignOp(
                op="insert",
                expected_index=None,
                actual_index=j - 1,
                expected_char="",
                actual_char=b[j - 1],
            )
        )
        j -= 1

    operations.reverse()
    match_count = sum(1 for op in operations if op.op == "match")
    substitution_count = sum(1 for op in operations if op.op == "substitute")
    insertion_count = sum(1 for op in operations if op.op == "insert")
    deletion_count = sum(1 for op in operations if op.op == "delete")
    edit_count = substitution_count + insertion_count + deletion_count
    denom = max(len(a), len(b), 1)
    match_rate = max(0.0, 1.0 - (edit_count / denom))

    return AlignmentResult(
        operations=operations,
        expected_len=len(a),
        actual_len=len(b),
        match_count=match_count,
        substitution_count=substitution_count,
        insertion_count=insertion_count,
        deletion_count=deletion_count,
        edit_count=edit_count,
        match_rate=match_rate,
    )


def normalize_pinyin(value: str) -> str:
    stripped = value.strip().lower()
    if not stripped:
        return ""
    stripped = stripped.replace("u:", "v").replace("ü", "v")
    decomposed = unicodedata.normalize("NFD", stripped)
    no_marks = "".join(ch for ch in decomposed if unicodedata.category(ch) != "Mn")
    return "".join(ch for ch in no_marks if not ch.isdigit() and ch.isalpha())


def pinyin_bases_for_char(char: str) -> set[str]:
    if not char:
        return set()
    values = pinyin(char, style=Style.NORMAL, heteronym=True, strict=False)
    if not values:
        return set()
    bases = {normalize_pinyin(item) for item in values[0]}
    return {base for base in bases if base and base != char}


def display_pinyin_for_char(char: str) -> str:
    bases = sorted(pinyin_bases_for_char(char))
    return "/".join(bases)


def classify_char_mismatch(
    *,
    expected_char: str,
    expected_pinyin: str,
    actual_char: str,
    expected_duration_ms: int | None = None,
) -> CharMismatch:
    if not expected_char and actual_char:
        return CharMismatch(
            expected_char="",
            expected_pinyin="",
            actual_char=actual_char,
            actual_pinyin=display_pinyin_for_char(actual_char),
            kind="asr_insertion",
            label="ASR 多听",
            counts_as_tts_error=False,
            note="ASR 多出字符，不能单独证明 TTS 错读。",
        )

    expected_base = normalize_pinyin(expected_pinyin) or next(iter(sorted(pinyin_bases_for_char(expected_char))), "")
    actual_bases = pinyin_bases_for_char(actual_char)
    actual_pinyin = "/".join(sorted(actual_bases))

    if expected_char and not actual_char:
        if expected_duration_ms is not None and expected_duration_ms <= 40:
            return CharMismatch(
                expected_char=expected_char,
                expected_pinyin=expected_pinyin,
                actual_char="",
                actual_pinyin="",
                kind="suspected_tts_omission",
                label="疑似 TTS 漏读",
                counts_as_tts_error=True,
                note="ASR 漏字且强制对齐给该字 0-40ms，优先按 TTS 漏读/吞字复检。",
            )
        return CharMismatch(
            expected_char=expected_char,
            expected_pinyin=expected_pinyin,
            actual_char="",
            actual_pinyin="",
            kind="asr_deletion",
            label="ASR 漏听",
            counts_as_tts_error=False,
            note="ASR 少识别字符，需结合复听判断。",
        )

    if expected_base and expected_base in actual_bases:
        return CharMismatch(
            expected_char=expected_char,
            expected_pinyin=expected_pinyin,
            actual_char=actual_char,
            actual_pinyin=actual_pinyin,
            kind="asr_homophone",
            label="ASR 同音误听",
            counts_as_tts_error=False,
            note="期望读音与 ASR 字同音，按小模型同音替字处理，不算 TTS 错。",
        )

    expected_readings = pinyin_bases_for_char(expected_char)
    alternate_readings = expected_readings - ({expected_base} if expected_base else set())
    if alternate_readings and actual_bases.intersection(alternate_readings):
        return CharMismatch(
            expected_char=expected_char,
            expected_pinyin=expected_pinyin,
            actual_char=actual_char,
            actual_pinyin=actual_pinyin,
            kind="suspected_tts_polyphone",
            label="疑似 TTS 多音字错读",
            counts_as_tts_error=True,
            note="ASR 字读音落在原字另一读音上，优先按 TTS 多音字错读复检。",
        )

    if len(expected_readings) > 1:
        return CharMismatch(
            expected_char=expected_char,
            expected_pinyin=expected_pinyin,
            actual_char=actual_char,
            actual_pinyin=actual_pinyin,
            kind="suspected_tts_polyphone",
            label="疑似 TTS 多音字错读",
            counts_as_tts_error=True,
            note="原字为多音字且 ASR 非同音，建议复听确认。",
        )

    return CharMismatch(
        expected_char=expected_char,
        expected_pinyin=expected_pinyin,
        actual_char=actual_char,
        actual_pinyin=actual_pinyin,
        kind="asr_nonhomophone_uncertain",
        label="非同音差异待复听",
        counts_as_tts_error=False,
        note="非同音差异；可能是 ASR 小模型误识，也可能是 TTS 咬字问题。",
    )


def sentence_plan_with_cells(story: dict[str, Any]) -> list[dict[str, Any]]:
    plan: list[dict[str, Any]] = []
    for para_index, paragraph in enumerate(story.get("paragraphs", [])):
        text = str(paragraph.get("text", ""))
        cells = paragraph.get("cells") if isinstance(paragraph.get("cells"), list) else []
        cursor = 0
        for sent_index, sentence in enumerate(split_paragraph_to_sentences(text)):
            start = text.find(sentence, cursor)
            if start < 0:
                start = cursor
            end = start + len(sentence)
            cursor = end
            cell_slice = cells[start:end] if len(cells) == len(text) else []
            pinyins = [str(cell.get("p", "")) for cell in cell_slice if isinstance(cell, dict)]
            if len(pinyins) != len(sentence):
                pinyins = [""] * len(sentence)
            plan.append(
                {
                    "paraIndex": para_index,
                    "sentIndex": sent_index,
                    "text": sentence,
                    "pinyins": pinyins,
                    "audioPath": f"audio/p{para_index + 1}_s{sent_index + 1}.wav",
                }
            )
    return plan


def discover_story_dirs(stories_root: Path, ids: list[str] | None) -> list[Path]:
    if ids:
        candidates = [stories_root / story_id for story_id in ids]
    else:
        candidates = sorted(path for path in stories_root.iterdir() if path.is_dir())
    return [
        path
        for path in candidates
        if (path / "story.json").exists() and (path / "audio.json").exists() and (path / "audio").is_dir()
    ]


def load_sentence_items(stories_root: Path, ids: list[str] | None = None) -> list[SentenceItem]:
    items: list[SentenceItem] = []
    for story_dir in discover_story_dirs(stories_root, ids):
        story = json.loads((story_dir / "story.json").read_text(encoding="utf-8"))
        story_id = str(story.get("id") or story_dir.name)
        title_zh = str(story.get("title_zh") or story_id)
        manifest = read_audio_manifest(story_dir / "audio.json")
        manifest_by_key = {
            (int(entry.get("paraIndex", -1)), int(entry.get("sentIndex", -1))): entry
            for entry in manifest
            if isinstance(entry, dict)
        }

        for expected in sentence_plan_with_cells(story):
            key = (expected["paraIndex"], expected["sentIndex"])
            entry = manifest_by_key.get(key, {})
            if entry.get("unavailable"):
                continue
            audio_rel = str(entry.get("audioPath") or expected["audioPath"])
            audio_path = story_dir / audio_rel
            if not audio_path.exists():
                continue
            expected_text = str(expected["text"])
            pinyins = expected["pinyins"]
            expected_norm = normalize_for_compare(expected_text, pinyins)
            char_durations = normalized_char_durations(
                expected_text=expected_text,
                expected_norm=expected_norm,
                manifest_chars=entry.get("chars"),
            )
            manifest_text = str(entry.get("text", ""))
            items.append(
                SentenceItem(
                    story_id=story_id,
                    title_zh=title_zh,
                    audio_path=str(story_dir.relative_to(REPO_ROOT) / audio_rel),
                    abs_audio_path=str(audio_path),
                    para_index=expected["paraIndex"],
                    sent_index=expected["sentIndex"],
                    expected_text=expected_text,
                    expected_norm=expected_norm,
                    expected_char_durations_ms=char_durations,
                    manifest_text=manifest_text,
                    manifest_mismatch=manifest_text != expected_text,
                    duration_ms=int(entry.get("durationMs") or 0),
                )
            )
    return items


def normalized_char_durations(
    *,
    expected_text: str,
    expected_norm: NormalizedText,
    manifest_chars: Any,
) -> list[int | None]:
    if not isinstance(manifest_chars, list) or len(manifest_chars) != len(expected_text):
        return [None] * len(expected_norm.chars)

    full_durations: list[int | None] = []
    for index, char in enumerate(expected_text):
        entry = manifest_chars[index]
        if not isinstance(entry, dict) or entry.get("c") != char:
            full_durations.append(None)
            continue
        try:
            start_ms = int(entry.get("startMs"))
            end_ms = int(entry.get("endMs"))
        except (TypeError, ValueError):
            full_durations.append(None)
            continue
        full_durations.append(max(0, end_ms - start_ms))

    return [
        full_durations[index] if 0 <= index < len(full_durations) else None
        for index in expected_norm.source_indices
    ]


def dtype_from_name(name: str) -> Any:
    import torch

    normalized = name.strip().lower()
    if normalized in {"float32", "fp32"}:
        return torch.float32
    if normalized in {"bfloat16", "bf16"}:
        return torch.bfloat16
    if normalized in {"float16", "fp16"}:
        return torch.float16
    raise ValueError(f"Unsupported dtype: {name}")


class QwenAsrTranscriber:
    def __init__(
        self,
        *,
        model_id: str,
        device_map: str,
        dtype: str,
        max_inference_batch_size: int,
        max_new_tokens: int,
        language: str,
    ) -> None:
        self.model_id = model_id
        self.device_map = device_map
        self.dtype = dtype
        self.max_inference_batch_size = max_inference_batch_size
        self.max_new_tokens = max_new_tokens
        self.language = language
        self._model: Any | None = None

    def _ensure_model(self) -> Any:
        if self._model is not None:
            return self._model
        from qwen_asr import Qwen3ASRModel

        self._model = Qwen3ASRModel.from_pretrained(
            self.model_id,
            device_map=self.device_map,
            dtype=dtype_from_name(self.dtype),
            max_inference_batch_size=self.max_inference_batch_size,
            max_new_tokens=self.max_new_tokens,
        )
        return self._model

    def transcribe(self, items: list[SentenceItem], *, contexts: list[str]) -> list[str]:
        if not items:
            return []
        model = self._ensure_model()
        results = model.transcribe(
            [item.abs_audio_path for item in items],
            context=contexts,
            language=[self.language] * len(items),
        )
        return [str(getattr(result, "text", "")).strip() for result in results]


def batched(values: list[Any], size: int) -> Iterable[list[Any]]:
    for index in range(0, len(values), size):
        yield values[index : index + size]


def transcribe_items(
    items: list[SentenceItem],
    *,
    transcriber: QwenAsrTranscriber,
    batch_size: int,
    confirm_mismatches: bool,
) -> list[SentenceAssessment]:
    assessments: list[SentenceAssessment] = []
    for chunk_index, chunk in enumerate(batched(items, batch_size), start=1):
        print(f"ASR context pass batch {chunk_index}: {len(chunk)} sentence(s)", flush=True)
        asr_texts = transcriber.transcribe(chunk, contexts=[item.expected_text for item in chunk])
        chunk_assessments = [
            assess_sentence(item, asr_text=asr_text, asr_no_context_text="")
            for item, asr_text in zip(chunk, asr_texts)
        ]

        if confirm_mismatches:
            mismatched = [
                (item, assessment)
                for item, assessment in zip(chunk, chunk_assessments)
                if assessment.edit_count > 0
            ]
            if mismatched:
                print(f"ASR no-context confirm batch {chunk_index}: {len(mismatched)} sentence(s)", flush=True)
                no_context_texts = transcriber.transcribe(
                    [pair[0] for pair in mismatched],
                    contexts=[""] * len(mismatched),
                )
                replacement_by_audio = {
                    item.audio_path: assess_sentence(
                        item,
                        asr_text=assessment.asr_text,
                        asr_no_context_text=no_context,
                    )
                    for (item, assessment), no_context in zip(mismatched, no_context_texts)
                }
                chunk_assessments = [
                    replacement_by_audio.get(assessment.audio_path, assessment)
                    for assessment in chunk_assessments
                ]

        assessments.extend(chunk_assessments)
    return assessments


def assess_sentence(
    item: SentenceItem,
    *,
    asr_text: str,
    asr_no_context_text: str,
) -> SentenceAssessment:
    actual_norm = normalize_for_compare(asr_text)
    alignment = align_normalized_text(item.expected_norm, actual_norm)
    char_issues: list[CharMismatch] = []

    for op in alignment.operations:
        if op.op == "match":
            continue
        expected_pinyin = ""
        expected_duration_ms = None
        if op.expected_index is not None and op.expected_index < len(item.expected_norm.pinyins):
            expected_pinyin = item.expected_norm.pinyins[op.expected_index]
        if op.expected_index is not None and op.expected_index < len(item.expected_char_durations_ms):
            expected_duration_ms = item.expected_char_durations_ms[op.expected_index]
        char_issues.append(
            classify_char_mismatch(
                expected_char=op.expected_char,
                expected_pinyin=expected_pinyin,
                actual_char=op.actual_char,
                expected_duration_ms=expected_duration_ms,
            )
        )

    judgment_code, judgment_label, recommendation = judge_sentence(
        char_issues=char_issues,
        asr_text=asr_text,
        asr_no_context_text=asr_no_context_text,
        expected_norm=item.expected_norm,
    )

    return SentenceAssessment(
        story_id=item.story_id,
        title_zh=item.title_zh,
        audio_path=item.audio_path,
        para_index=item.para_index,
        sent_index=item.sent_index,
        expected_text=item.expected_text,
        asr_text=asr_text,
        asr_no_context_text=asr_no_context_text,
        expected_norm=item.expected_norm.text,
        asr_norm=actual_norm.text,
        match_rate=alignment.match_rate,
        edit_count=alignment.edit_count,
        expected_len=alignment.expected_len,
        actual_len=alignment.actual_len,
        mismatch_summary=summarize_char_issues(char_issues),
        judgment_code=judgment_code,
        judgment_label=judgment_label,
        recommendation=recommendation,
        manifest_mismatch=item.manifest_mismatch,
        duration_ms=item.duration_ms,
        char_issues=[asdict(issue) for issue in char_issues],
    )


def judge_sentence(
    *,
    char_issues: list[CharMismatch],
    asr_text: str,
    asr_no_context_text: str,
    expected_norm: NormalizedText,
) -> tuple[str, str, str]:
    if not char_issues:
        return "pass", "通过", "无需处理。"

    if any(issue.kind == "suspected_tts_polyphone" for issue in char_issues):
        chars = "、".join(sorted({issue.expected_char for issue in char_issues if issue.counts_as_tts_error}))
        return (
            "suspected_tts_polyphone",
            "疑似 TTS 多音字错读",
            f"建议优先重生成该句；若仍错读，改写规避多音字 {chars}。",
        )

    if any(issue.kind == "suspected_tts_omission" for issue in char_issues):
        chars = "、".join(sorted({issue.expected_char for issue in char_issues if issue.kind == "suspected_tts_omission"}))
        return (
            "suspected_tts_omission",
            "疑似 TTS 漏读/吞字",
            f"建议优先复听并重生成该句；若仍吞字，改写规避 {chars} 所在短语。",
        )

    non_homophone = [issue for issue in char_issues if issue.kind == "asr_nonhomophone_uncertain"]
    if non_homophone:
        no_context_norm = normalize_for_compare(asr_no_context_text).text if asr_no_context_text else ""
        context_norm = normalize_for_compare(asr_text).text
        if no_context_norm and no_context_norm == context_norm and context_norm != expected_norm.text:
            return (
                "suspected_tts_nonhomophone",
                "疑似 TTS 非同音错读",
                "建议人工复听；确认后重生成，或把该处改写为更常见表达。",
            )
        return (
            "needs_review_nonhomophone",
            "非同音差异待复听",
            "先人工复听；若耳听原音正确，则保留为 ASR 小模型误识。",
        )

    if all(issue.kind == "asr_homophone" for issue in char_issues):
        return (
            "asr_homophone",
            "ASR 小模型同音误听",
            "不建议重生成；同音替字不算 TTS 错。",
        )

    return (
        "asr_uncertain",
        "ASR 漏听/多听待观察",
        "不直接判 TTS 错；可抽样复听低匹配句。",
    )


def summarize_char_issues(char_issues: list[CharMismatch]) -> str:
    if not char_issues:
        return "-"
    parts: list[str] = []
    for issue in char_issues[:8]:
        expected = issue.expected_char or "∅"
        actual = issue.actual_char or "∅"
        expected_py = f"({issue.expected_pinyin})" if issue.expected_pinyin else ""
        actual_py = f"({issue.actual_pinyin})" if issue.actual_pinyin else ""
        parts.append(f"{expected}{expected_py}->{actual}{actual_py}:{issue.label}")
    if len(char_issues) > 8:
        parts.append(f"...(+{len(char_issues) - 8})")
    return "; ".join(parts)


def aggregate(assessments: list[SentenceAssessment]) -> dict[str, Any]:
    total_expected = sum(item.expected_len for item in assessments)
    total_max_len = sum(max(item.expected_len, item.actual_len, 1) for item in assessments)
    total_edits = sum(item.edit_count for item in assessments)
    total_match_rate = 1.0 - (total_edits / total_max_len) if total_max_len else 1.0
    mismatches = [item for item in assessments if item.edit_count > 0]
    return {
        "sentence_count": len(assessments),
        "expected_chars": total_expected,
        "edits": total_edits,
        "match_rate": total_match_rate,
        "mismatch_sentence_count": len(mismatches),
        "asr_homophone_count": sum(1 for item in mismatches if item.judgment_code == "asr_homophone"),
        "suspected_tts_count": sum(
            1 for item in mismatches if item.judgment_code.startswith("suspected_tts")
        ),
        "needs_review_count": sum(1 for item in mismatches if item.judgment_code.startswith("needs_review")),
    }


def story_rows(assessments: list[SentenceAssessment]) -> list[dict[str, Any]]:
    by_story: dict[str, list[SentenceAssessment]] = {}
    for item in assessments:
        by_story.setdefault(item.story_id, []).append(item)

    rows: list[dict[str, Any]] = []
    for story_id, story_items in sorted(by_story.items()):
        stats = aggregate(story_items)
        rows.append(
            {
                "story_id": story_id,
                "title_zh": story_items[0].title_zh if story_items else story_id,
                **stats,
            }
        )
    return rows


def markdown_escape(value: str) -> str:
    return value.replace("|", "\\|").replace("\n", " ").strip()


def percent(value: float) -> str:
    return f"{value * 100:.2f}%"


def write_markdown_report(
    *,
    output_path: Path,
    raw_path: Path,
    assessments: list[SentenceAssessment],
    args: argparse.Namespace,
) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    stats = aggregate(assessments)
    rows = story_rows(assessments)
    mismatches = [item for item in assessments if item.edit_count > 0]
    suspected = [
        item
        for item in mismatches
        if item.judgment_code
        in {"suspected_tts_polyphone", "suspected_tts_nonhomophone", "suspected_tts_omission"}
    ]
    suspected.sort(key=lambda item: (item.judgment_code != "suspected_tts_polyphone", item.match_rate))

    lines: list[str] = []
    lines.append("# TTS ASR 回环发音验证")
    lines.append("")
    lines.append(f"- 生成时间：{datetime.now().isoformat(timespec='seconds')}")
    lines.append(f"- ASR：`{args.model}`，language=`{args.language}`，device=`{args.device_map}`，dtype=`{args.dtype}`")
    lines.append("- 方法：逐句 wav 先带原文 context 转写；不匹配句再无 context 复核；字符级比对忽略标点和空白。")
    lines.append(f"- 原始结果：`{raw_path.relative_to(REPO_ROOT)}`")
    lines.append("")
    lines.append("## 总结")
    lines.append("")
    lines.append(f"- 总句数：{stats['sentence_count']}")
    lines.append(f"- 总字符匹配率：{percent(stats['match_rate'])}（忽略标点；{stats['edits']} edit / {stats['expected_chars']} 期望字符）")
    lines.append(f"- 不匹配句：{stats['mismatch_sentence_count']}")
    lines.append(f"- ASR 小模型同音误听：{stats['asr_homophone_count']} 句")
    lines.append(f"- 疑似 TTS 错读：{stats['suspected_tts_count']} 句")
    lines.append(f"- 非同音待复听：{stats['needs_review_count']} 句")
    lines.append("")

    if suspected:
        lines.append("## Top 疑似 TTS 误读")
        lines.append("")
        for item in suspected[:10]:
            lines.append(
                f"- `{item.audio_path}` {item.title_zh}：{item.mismatch_summary}；"
                f"原文「{item.expected_text}」；ASR「{item.asr_text}」。{item.recommendation}"
            )
    else:
        lines.append("## Top 疑似 TTS 误读")
        lines.append("")
        lines.append("- 未发现高置信疑似 TTS 错读；现有差异按 ASR 同音误听或待复听处理。")
    lines.append("")

    lines.append("## 逐篇匹配率")
    lines.append("")
    lines.append("| Story | 标题 | 句数 | 匹配率 | 不匹配句 | ASR 同音误听 | 疑似 TTS | 待复听 |")
    lines.append("|---|---:|---:|---:|---:|---:|---:|---:|")
    for row in rows:
        lines.append(
            "| "
            + " | ".join(
                [
                    f"`{markdown_escape(row['story_id'])}`",
                    markdown_escape(row["title_zh"]),
                    str(row["sentence_count"]),
                    percent(row["match_rate"]),
                    str(row["mismatch_sentence_count"]),
                    str(row["asr_homophone_count"]),
                    str(row["suspected_tts_count"]),
                    str(row["needs_review_count"]),
                ]
            )
            + " |"
        )
    lines.append("")

    lines.append("## 问题句清单")
    lines.append("")
    if not mismatches:
        lines.append("- 无。")
    else:
        current_story = ""
        for item in sorted(mismatches, key=lambda value: (value.story_id, value.para_index, value.sent_index)):
            if item.story_id != current_story:
                current_story = item.story_id
                lines.append(f"### {item.title_zh} (`{item.story_id}`)")
                lines.append("")
                lines.append("| 音频 | 匹配率 | 原文 | ASR(context) | ASR(no context) | 疑似误读字 | 判断 | 建议 |")
                lines.append("|---|---:|---|---|---|---|---|---|")
            lines.append(
                "| "
                + " | ".join(
                    [
                        f"`{markdown_escape(item.audio_path)}`",
                        percent(item.match_rate),
                        markdown_escape(item.expected_text),
                        markdown_escape(item.asr_text),
                        markdown_escape(item.asr_no_context_text or "-"),
                        markdown_escape(item.mismatch_summary),
                        markdown_escape(item.judgment_label),
                        markdown_escape(item.recommendation),
                    ]
                )
                + " |"
            )
        lines.append("")

    lines.append("## 结论")
    lines.append("")
    if suspected:
        lines.append("- 建议优先复听并重生成 Top 疑似 TTS 误读中的句子；若同一多音字或短语重复错读/吞字，改写为更常见、更不易误读的表达。")
    else:
        lines.append("- 本轮未发现需要立即重生成的高置信 TTS 误读。")
    lines.append("- 同音替字（如期望字与 ASR 字拼音相同）按 ASR 小模型误听处理，不计入 TTS 错。")
    lines.append("- 非同音但未命中多音字的差异需要人工复听；若耳听 TTS 正确，可保留为 ASR 误识记录。")
    lines.append("")

    output_path.write_text("\n".join(lines), encoding="utf-8")


def write_raw(
    *,
    raw_path: Path,
    assessments: list[SentenceAssessment],
    args: argparse.Namespace,
) -> None:
    raw_path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "generated_at": datetime.now().isoformat(timespec="seconds"),
        "model": args.model,
        "language": args.language,
        "device_map": args.device_map,
        "dtype": args.dtype,
        "assessments": [asdict(item) for item in assessments],
    }
    raw_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def read_raw(raw_path: Path) -> list[SentenceAssessment]:
    payload = json.loads(raw_path.read_text(encoding="utf-8"))
    return [SentenceAssessment(**item) for item in payload.get("assessments", [])]


def reassess_from_raw(
    *,
    raw_assessments: list[SentenceAssessment],
    stories_root: Path,
    ids: list[str] | None,
) -> list[SentenceAssessment]:
    items = load_sentence_items(stories_root, ids)
    item_by_audio = {item.audio_path: item for item in items}
    reassessed: list[SentenceAssessment] = []
    for previous in raw_assessments:
        item = item_by_audio.get(previous.audio_path)
        if item is None:
            reassessed.append(previous)
            continue
        reassessed.append(
            assess_sentence(
                item,
                asr_text=previous.asr_text,
                asr_no_context_text=previous.asr_no_context_text,
            )
        )
    return reassessed


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run Qwen3-ASR loopback checks for story TTS wav files.")
    parser.add_argument("--stories-root", default=str(REPO_ROOT / "content" / "stories"))
    parser.add_argument("--ids", nargs="*", help="Optional story ids to check.")
    parser.add_argument("--model", default="Qwen/Qwen3-ASR-0.6B")
    parser.add_argument("--language", default="Chinese")
    parser.add_argument("--device-map", default="cpu")
    parser.add_argument("--dtype", default="float32")
    parser.add_argument("--batch-size", type=int, default=4)
    parser.add_argument("--max-new-tokens", type=int, default=128)
    parser.add_argument("--output", default=str(REPO_ROOT / "docs" / "qa" / "tts-asr-verification.md"))
    parser.add_argument("--raw-output", default=str(REPO_ROOT / "docs" / "qa" / "tts-asr-verification.raw.json"))
    parser.add_argument("--reuse-raw", action="store_true", help="Regenerate markdown from --raw-output without ASR.")
    parser.add_argument("--skip-confirm", action="store_true", help="Skip no-context confirmation for mismatches.")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    stories_root = Path(args.stories_root)
    output_path = Path(args.output)
    raw_path = Path(args.raw_output)

    if args.reuse_raw:
        assessments = reassess_from_raw(
            raw_assessments=read_raw(raw_path),
            stories_root=stories_root,
            ids=args.ids,
        )
        write_raw(raw_path=raw_path, assessments=assessments, args=args)
    else:
        items = load_sentence_items(stories_root, args.ids)
        print(f"Loaded {len(items)} sentence wav(s) from {stories_root}", flush=True)
        transcriber = QwenAsrTranscriber(
            model_id=args.model,
            device_map=args.device_map,
            dtype=args.dtype,
            max_inference_batch_size=args.batch_size,
            max_new_tokens=args.max_new_tokens,
            language=args.language,
        )
        assessments = transcribe_items(
            items,
            transcriber=transcriber,
            batch_size=args.batch_size,
            confirm_mismatches=not args.skip_confirm,
        )
        write_raw(raw_path=raw_path, assessments=assessments, args=args)

    write_markdown_report(
        output_path=output_path,
        raw_path=raw_path,
        assessments=assessments,
        args=args,
    )
    stats = aggregate(assessments)
    print(
        f"Wrote {output_path}. Total match rate: {percent(stats['match_rate'])}; "
        f"mismatches: {stats['mismatch_sentence_count']}; suspected TTS: {stats['suspected_tts_count']}",
        flush=True,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
