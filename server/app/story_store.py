import json
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_STORIES_ROOT = REPO_ROOT / "content" / "stories"


class StoryNotFoundError(Exception):
    pass


@dataclass(frozen=True)
class VocabItem:
    word: str
    pinyin: str
    meaning: str
    example: str = ""

    def get(self, name: str, default: Any = None) -> Any:
        return getattr(self, name, default)


@dataclass(frozen=True)
class QuestionItem:
    id: str
    type: str
    prompt: str
    options: tuple[str, ...]
    answer: str
    explanation: str

    def get(self, name: str, default: Any = None) -> Any:
        return getattr(self, name, default)


@dataclass(frozen=True)
class StoryContext:
    id: str
    title_zh: str
    title_en: str
    body: str
    vocab: tuple[VocabItem, ...]
    questions: tuple[QuestionItem, ...]
    retell_prompt: str

    @classmethod
    def from_json(cls, data: dict[str, Any]) -> "StoryContext":
        paragraphs = data.get("paragraphs", [])
        body = "\n".join(str(item.get("text", "")) for item in paragraphs)
        return cls(
            id=str(data["id"]),
            title_zh=str(data["title_zh"]),
            title_en=str(data["title_en"]),
            body=body,
            vocab=tuple(
                VocabItem(
                    word=str(item.get("word", "")),
                    pinyin=str(item.get("pinyin", "")),
                    meaning=str(item.get("meaning", "")),
                    example=str(item.get("example", "")),
                )
                for item in data.get("vocab", [])
            ),
            questions=tuple(
                QuestionItem(
                    id=str(item.get("id", "")),
                    type=str(item.get("type", "")),
                    prompt=str(item.get("prompt", "")),
                    options=tuple(str(option) for option in item.get("options", [])),
                    answer=str(item.get("answer", "")),
                    explanation=str(item.get("explanation", "")),
                )
                for item in data.get("questions", [])
            ),
            retell_prompt=str(data.get("retell_prompt", "")),
        )

    @property
    def title(self) -> str:
        return f"{self.title_zh} / {self.title_en}"

    @property
    def question_text(self) -> str:
        parts: list[str] = []
        for question in self.questions:
            parts.append(str(question.get("prompt", "")))
            parts.extend(str(option) for option in question.get("options", []))
            parts.append(str(question.get("answer", "")))
            parts.append(str(question.get("explanation", "")))
        return "\n".join(parts)

    @property
    def vocab_text(self) -> str:
        parts: list[str] = []
        for item in self.vocab:
            parts.append(str(item.get("word", "")))
            parts.append(str(item.get("pinyin", "")))
            parts.append(str(item.get("meaning", "")))
            parts.append(str(item.get("example", "")))
        return "\n".join(parts)

    @property
    def corpus(self) -> str:
        return "\n".join(
            [self.title_zh, self.title_en, self.body, self.vocab_text, self.question_text, self.retell_prompt]
        )


class StoryStore:
    def __init__(self, stories_root: Path = DEFAULT_STORIES_ROOT) -> None:
        self._stories_root = stories_root

    @lru_cache(maxsize=128)
    def load(self, story_id: str) -> StoryContext:
        story_path = self._stories_root / story_id / "story.json"
        if not story_path.is_file():
            raise StoryNotFoundError(story_id)
        with story_path.open("r", encoding="utf-8") as handle:
            data = json.load(handle)
        return StoryContext.from_json(data)

    def get_story(self, story_id: str) -> StoryContext:
        return self.load(story_id)
