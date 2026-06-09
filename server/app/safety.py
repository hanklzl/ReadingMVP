from __future__ import annotations

import hashlib
import html
import re
import threading
import time
import unicodedata
from collections import deque
from dataclasses import asdict, is_dataclass
from typing import Any


REFUSAL_MESSAGE = "这个问题和今天的故事关系不大，我们先回到故事里吧。"

MAX_SELECTED_TEXT_CHARS = 160
MAX_ANSWER_CHARS = 100
MAX_RATE_LIMIT_KEY_CHARS = 128
MAX_STORY_CONTEXT_CHARS = 20_000
_MAX_RATE_LIMIT_BUCKETS = 10_000

_EMAIL_RE = re.compile(r"\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b", re.IGNORECASE)
_PHONE_RE = re.compile(
    r"(?<!\d)(?:\+?\d{1,3}[-.\s]?)?(?:\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}|1[3-9]\d{9})(?!\d)"
)
_CONTROL_RE = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]")
_HTML_TAG_RE = re.compile(r"<[^>]*>")
_URL_RE = re.compile(r"https?://|www\.", re.IGNORECASE)
_ASCII_WORD_RE = re.compile(r"[A-Za-z][A-Za-z'-]{1,}")
_CJK_RE = re.compile(r"[\u3400-\u9fff]+")
_NON_SCOPE_CHAR_RE = re.compile(r"[^0-9A-Za-z\u3400-\u9fff]+")

_PII_PATTERNS = (
    re.compile(r"(?:我|孩子|宝宝|小孩|女儿|儿子)\s*(?:叫|名叫|的名字是|姓名是)\s*[\u3400-\u9fffA-Za-z][\u3400-\u9fffA-Za-z\s]{1,20}"),
    re.compile(r"(?:我的|孩子的|宝宝的|女儿的|儿子的)?\s*(?:真实姓名|家庭住址|住址|地址|电话号码|手机号|学校|班级|生日|出生日期|身份证|护照|社保号)"),
    re.compile(r"(?:我|孩子|宝宝|小孩|女儿|儿子)\s*(?:住在|在.*(?:小学|学校|幼儿园|班))"),
    re.compile(r"(?:我|孩子|宝宝|小孩|女儿|儿子)\s*(?:今年)?\s*\d{1,2}\s*岁"),
    re.compile(r"\b\d{1,6}\s+[A-Za-z0-9 .'-]{2,}\s+(?:street|st|road|rd|avenue|ave|drive|dr|lane|ln|blvd|boulevard)\b", re.IGNORECASE),
)

_UNSAFE_INPUT_PATTERNS = (
    re.compile(r"(?:成人内容|色情|黄色|裸露|性行为|毒品|赌博|武器制作|炸弹)"),
    re.compile(r"(?:恐怖|吓人|鬼|僵尸|恶灵|噩梦).{0,12}(?:故事|细节|描写|画面)?"),
    re.compile(r"(?:血腥|流血|尸体|砍头|肢解|虐待|自杀|杀人|怎么杀|暴力细节|残忍)"),
    re.compile(r"(?:详细|具体|逼真).{0,8}(?:打斗|战斗|战争|受伤|死亡|杀)"),
)
_UNSAFE_OUTPUT_PATTERNS = _UNSAFE_INPUT_PATTERNS

_OFF_TOPIC_PATTERNS = (
    re.compile(r"(?:午饭|晚饭|早餐|吃什么|点外卖|天气|天气预报|新闻|股票|游戏|游戏充值|作业答案|写作业|编程|代码|旅游|买什么|密码)"),
    re.compile(r"(?:陪我|随便|开放|自由).{0,8}(?:聊天|聊聊|对话)"),
    re.compile(r"(?:你是谁|你叫什么|讲个笑话|唱首歌|写首诗|画一张|扮演|角色扮演)"),
    re.compile(r"(?:我的|我家|我妈妈|我爸爸|我朋友|我的朋友).{0,16}(?:怎么办|怎么做|是谁|在哪|住哪|电话|邮箱|名字|地址)"),
)

_COMMON_TERMS = {
    "一个",
    "一些",
    "这个",
    "那个",
    "这些",
    "那些",
    "什么",
    "怎么",
    "怎样",
    "为什么",
    "因为",
    "所以",
    "可以",
    "知道",
    "觉得",
    "今天",
    "明天",
    "昨天",
    "大家",
    "我们",
    "他们",
    "你们",
    "自己",
    "一起",
    "应该",
    "故事",
    "问题",
    "人物",
    "意思",
}
_QUESTION_HINTS = ("什么", "意思", "为什么", "怎么", "怎样", "谁", "哪里", "哪", "吗", "呢", "解释", "说明")
_ASCII_STOPWORDS = {"the", "and", "for", "with", "that", "this", "what", "why", "how", "who", "where", "does", "mean"}


def contains_pii(text: str) -> bool:
    if not isinstance(text, str):
        return False
    normalized = _normalize_text(text)
    if not normalized:
        return False
    if _EMAIL_RE.search(normalized) or _PHONE_RE.search(normalized):
        return True
    return any(pattern.search(normalized) for pattern in _PII_PATTERNS)


def contains_blocked_topic(text: str) -> bool:
    if not isinstance(text, str):
        return True
    normalized = _normalize_text(text)
    return _has_unsafe_content(normalized) or _is_obviously_off_topic(normalized)


def validate_story_scope(
    selected_text: str,
    story: Any,
    question_type: str | None = None,
    *extra_args: Any,
    **extra_kwargs: Any,
) -> bool:
    del question_type, extra_args, extra_kwargs
    if not isinstance(selected_text, str) and isinstance(story, str):
        selected_text, story = story, selected_text
    if not isinstance(selected_text, str):
        return False

    text = _normalize_text(selected_text)
    if not text or len(text) > MAX_SELECTED_TEXT_CHARS:
        return False
    if _has_control_chars(text) or contains_pii(text) or contains_blocked_topic(text):
        return False

    story_parts = _story_parts(story)
    story_context = " ".join(story_parts.values())
    if not story_context:
        return False

    compact_text = _compact(text)
    compact_context = _compact(story_context)
    if len(compact_text) >= 2 and compact_text in compact_context:
        return True

    protected_terms = _text_terms(story_parts["title"]) | _text_terms(story_parts["vocab"]) | _text_terms(story_parts["questions"])
    if _terms_in_text(protected_terms, compact_text, text):
        return True

    body_matches = _matched_terms(_text_terms(story_parts["body"]), compact_text, text)
    if len(body_matches) >= 2:
        return True
    if body_matches and any(hint in text for hint in _QUESTION_HINTS):
        return True

    return _ascii_story_overlap(text, story_context)


def sanitize_answer(answer: str) -> str:
    if not isinstance(answer, str):
        return REFUSAL_MESSAGE
    cleaned = html.unescape(answer)
    cleaned = _HTML_TAG_RE.sub("", cleaned)
    cleaned = _normalize_text(cleaned)
    if not cleaned:
        return REFUSAL_MESSAGE
    if _has_control_chars(cleaned) or contains_pii(cleaned) or _has_unsafe_content(cleaned):
        return REFUSAL_MESSAGE
    if cleaned == REFUSAL_MESSAGE:
        return REFUSAL_MESSAGE
    return _truncate_answer(cleaned)


class InMemoryRateLimiter:
    def __init__(self, max_requests: int = 30, window_seconds: int = 60):
        if max_requests < 1:
            raise ValueError("max_requests must be at least 1")
        if window_seconds < 1:
            raise ValueError("window_seconds must be at least 1")
        self.max_requests = int(max_requests)
        self.window_seconds = int(window_seconds)
        self._hits: dict[str, deque[float]] = {}
        self._lock = threading.Lock()

    def check(self, key: str) -> bool:
        if not isinstance(key, str):
            return False
        normalized_key = _normalize_text(key)
        if not normalized_key or len(normalized_key) > MAX_RATE_LIMIT_KEY_CHARS or _has_control_chars(normalized_key):
            return False

        now = time.monotonic()
        cutoff = now - self.window_seconds
        bucket_key = hashlib.sha256(normalized_key.encode("utf-8")).hexdigest()

        with self._lock:
            bucket = self._hits.get(bucket_key)
            if bucket is None:
                if len(self._hits) >= _MAX_RATE_LIMIT_BUCKETS:
                    self._prune_expired(cutoff)
                if len(self._hits) >= _MAX_RATE_LIMIT_BUCKETS:
                    return False
                bucket = deque()
                self._hits[bucket_key] = bucket

            while bucket and bucket[0] <= cutoff:
                bucket.popleft()
            if len(bucket) >= self.max_requests:
                return False
            bucket.append(now)
            return True

    def _prune_expired(self, cutoff: float) -> None:
        expired_keys = []
        for key, bucket in self._hits.items():
            while bucket and bucket[0] <= cutoff:
                bucket.popleft()
            if not bucket:
                expired_keys.append(key)
        for key in expired_keys:
            del self._hits[key]


def _normalize_text(text: str) -> str:
    normalized = unicodedata.normalize("NFKC", text)
    return " ".join(normalized.strip().split())


def _has_control_chars(text: str) -> bool:
    return bool(_CONTROL_RE.search(text))


def _has_unsafe_content(text: str) -> bool:
    return any(pattern.search(text) for pattern in _UNSAFE_INPUT_PATTERNS)


def _is_obviously_off_topic(text: str) -> bool:
    if _URL_RE.search(text):
        return True
    return any(pattern.search(text) for pattern in _OFF_TOPIC_PATTERNS)


def _story_parts(story: Any) -> dict[str, str]:
    title_parts = [
        _story_value(story, "title", ""),
        _story_value(story, "title_zh", ""),
        _story_value(story, "title_en", ""),
    ]
    body_parts = [
        _story_value(story, "body", ""),
        _story_value(story, "paragraphs", ""),
    ]
    vocab_parts = [
        _story_value(story, "vocab", ""),
        _story_value(story, "vocab_text", ""),
    ]
    question_parts = [
        _story_value(story, "questions", ""),
        _story_value(story, "question_text", ""),
    ]
    return {
        "title": _flatten_limited(title_parts),
        "body": _flatten_limited(body_parts),
        "vocab": _flatten_limited(vocab_parts),
        "questions": _flatten_limited(question_parts),
        "corpus": _flatten_limited(_story_value(story, "corpus", "")),
    }


def _story_value(story: Any, name: str, default: Any) -> Any:
    if story is None:
        return default
    if isinstance(story, dict):
        return story.get(name, default)
    if is_dataclass(story):
        data = asdict(story)
        if name in data:
            return data[name]
    if hasattr(story, "model_dump"):
        dumped = story.model_dump()
        if isinstance(dumped, dict) and name in dumped:
            return dumped[name]
    return getattr(story, name, default)


def _flatten_limited(value: Any, limit: int = MAX_STORY_CONTEXT_CHARS) -> str:
    chunks: list[str] = []
    total = 0

    def visit(item: Any) -> None:
        nonlocal total
        if total >= limit or item is None:
            return
        if isinstance(item, str):
            text = _normalize_text(item)
            if text:
                remaining = limit - total
                chunks.append(text[:remaining])
                total += min(len(text), remaining)
            return
        if isinstance(item, dict):
            for child in item.values():
                visit(child)
                if total >= limit:
                    return
            return
        if isinstance(item, (list, tuple, set)):
            for child in item:
                visit(child)
                if total >= limit:
                    return
            return
        if is_dataclass(item):
            visit(asdict(item))

    visit(value)
    return " ".join(chunks)


def _compact(text: str) -> str:
    return _NON_SCOPE_CHAR_RE.sub("", unicodedata.normalize("NFKC", text)).lower()


def _text_terms(text: str) -> set[str]:
    terms: set[str] = set()
    for chunk in _CJK_RE.findall(_compact(text)):
        if 2 <= len(chunk) <= 4:
            terms.add(chunk)
            continue
        for length in range(2, min(4, len(chunk)) + 1):
            for index in range(len(chunk) - length + 1):
                terms.add(chunk[index : index + length])
    terms.update(
        word.lower()
        for word in _ASCII_WORD_RE.findall(text)
        if len(word) >= 3 and word.lower() not in _ASCII_STOPWORDS
    )
    return {term for term in terms if term and term not in _COMMON_TERMS}


def _terms_in_text(terms: set[str], compact_text: str, raw_text: str) -> bool:
    return bool(_matched_terms(terms, compact_text, raw_text))


def _matched_terms(terms: set[str], compact_text: str, raw_text: str) -> set[str]:
    raw_words = {word.lower() for word in _ASCII_WORD_RE.findall(raw_text)}
    return {
        term
        for term in terms
        if (_CJK_RE.fullmatch(term) and term in compact_text) or (not _CJK_RE.fullmatch(term) and term.lower() in raw_words)
    }


def _ascii_story_overlap(text: str, story_context: str) -> bool:
    query_words = {
        word.lower()
        for word in _ASCII_WORD_RE.findall(text)
        if len(word) >= 3 and word.lower() not in _ASCII_STOPWORDS
    }
    if not query_words:
        return False
    return bool(query_words & _text_terms(story_context))


def _truncate_answer(text: str) -> str:
    if len(text) <= MAX_ANSWER_CHARS:
        return text
    truncated = text[:MAX_ANSWER_CHARS].rstrip()
    for separator in ("。", "！", "？", ".", "!", "?"):
        position = truncated.rfind(separator)
        if position >= MAX_ANSWER_CHARS // 2:
            return truncated[: position + 1]
    return truncated
