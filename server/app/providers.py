from __future__ import annotations

import os
from abc import ABC, abstractmethod
from typing import Any

import httpx

from app.models import ExplainRequest, QuestionType
from app.safety import MAX_ANSWER_CHARS, REFUSAL_MESSAGE, sanitize_answer


class ExplainProvider(ABC):
    @abstractmethod
    def explain(self, request: ExplainRequest, story: Any, prompt: str) -> str:
        raise NotImplementedError


class MockExplainProvider(ExplainProvider):
    def explain(self, request: ExplainRequest, story: Any, prompt: str) -> str:
        if request.question_type is QuestionType.EXPLAIN_WORD:
            return sanitize_answer(self._explain_word(request.selected_text, story))
        if request.question_type is QuestionType.EXPLAIN_SENTENCE:
            return sanitize_answer(self._explain_sentence(request.selected_text, story))
        return sanitize_answer(self._answer_question(request.selected_text, story))

    def _explain_word(self, selected_text: str, story: Any) -> str:
        vocab_item = _find_vocab(selected_text, story)
        if vocab_item:
            word = vocab_item.get("word", selected_text)
            example = vocab_item.get("example", "")
            return f"“{word}”在故事里是一个重要词。你可以看这句：{example} 它帮助我们读懂人物的想法。"
        sentence = _find_sentence(selected_text, story)
        if sentence:
            return f"“{selected_text}”和故事内容有关。这里是在说：{_soft_summary(sentence)}"
        return _theme_answer(story)

    def _explain_sentence(self, selected_text: str, story: Any) -> str:
        sentence = _find_sentence(selected_text, story) or selected_text
        return f"这句话是在说：{_soft_summary(sentence)}读的时候，可以想想人物怎样做出好选择。"

    def _answer_question(self, selected_text: str, story: Any) -> str:
        matched = _match_question(selected_text, story)
        if matched:
            answer = matched.get("answer", "")
            explanation = matched.get("explanation", "")
            return f"答案是“{answer}”。{explanation}"
        return _theme_answer(story)


class OpenAIExplainProvider(ExplainProvider):
    def __init__(self) -> None:
        self._api_key = os.getenv("OPENAI_API_KEY")
        self._model = os.getenv("OPENAI_MODEL")
        if not self._api_key or not self._model:
            raise RuntimeError("OPENAI_API_KEY and OPENAI_MODEL must be set for AI_PROVIDER=openai.")

    def explain(self, request: ExplainRequest, story: Any, prompt: str) -> str:
        response = httpx.post(
            "https://api.openai.com/v1/chat/completions",
            headers={"Authorization": f"Bearer {self._api_key}"},
            json={
                "model": self._model,
                "messages": [
                    {"role": "system", "content": prompt},
                    {"role": "user", "content": request.selected_text},
                ],
                "max_tokens": 160,
                "temperature": 0.2,
            },
            timeout=20.0,
        )
        response.raise_for_status()
        data = response.json()
        content = data["choices"][0]["message"]["content"]
        return sanitize_answer(content)


class AnthropicExplainProvider(ExplainProvider):
    def __init__(self) -> None:
        self._api_key = os.getenv("ANTHROPIC_API_KEY")
        self._model = os.getenv("ANTHROPIC_MODEL")
        if not self._api_key or not self._model:
            raise RuntimeError("ANTHROPIC_API_KEY and ANTHROPIC_MODEL must be set for AI_PROVIDER=anthropic.")

    def explain(self, request: ExplainRequest, story: Any, prompt: str) -> str:
        response = httpx.post(
            "https://api.anthropic.com/v1/messages",
            headers={
                "x-api-key": self._api_key,
                "anthropic-version": os.getenv("ANTHROPIC_VERSION", "2023-06-01"),
            },
            json={
                "model": self._model,
                "system": prompt,
                "messages": [{"role": "user", "content": request.selected_text}],
                "max_tokens": 160,
                "temperature": 0.2,
            },
            timeout=20.0,
        )
        response.raise_for_status()
        data = response.json()
        content = "".join(item.get("text", "") for item in data.get("content", []) if item.get("type") == "text")
        return sanitize_answer(content)


def get_provider() -> ExplainProvider:
    provider_name = os.getenv("AI_EXPLAIN_PROVIDER") or os.getenv("AI_PROVIDER", "mock")
    provider_name = provider_name.strip().lower()
    if provider_name in {"mock", "auto"}:
        return MockExplainProvider()
    if provider_name == "openai":
        return OpenAIExplainProvider()
    if provider_name == "anthropic":
        return AnthropicExplainProvider()
    raise RuntimeError("AI_EXPLAIN_PROVIDER must be one of: mock, auto, openai, anthropic.")


def _find_vocab(selected_text: str, story: Any) -> Any | None:
    for item in _story_value(story, "vocab", ()):
        word = str(_item_value(item, "word", ""))
        if word and (word in selected_text or selected_text in word):
            return item
    return None


def _find_sentence(selected_text: str, story: Any) -> str | None:
    body = str(_story_value(story, "body", ""))
    for sentence in body.replace("！", "。").replace("？", "。").split("。"):
        sentence = sentence.strip()
        if sentence and (selected_text in sentence or sentence in selected_text):
            return sentence
    return None


def _match_question(selected_text: str, story: Any) -> Any | None:
    for question in _story_value(story, "questions", ()):
        prompt = str(_item_value(question, "prompt", ""))
        answer = str(_item_value(question, "answer", ""))
        if prompt and (selected_text in prompt or prompt in selected_text):
            return question
        if answer and answer in selected_text:
            return question
    return None


def _theme_answer(story: Any) -> str:
    title = _story_value(story, "title_zh", "这个故事")
    retell = _story_value(story, "retell_prompt", "")
    if retell:
        return f"在《{title}》里，可以从人物的做法想一想：{retell} 重点是学习善良、合作和沉着。"
    return REFUSAL_MESSAGE


def _soft_summary(sentence: str) -> str:
    cleaned = " ".join(sentence.split())
    if len(cleaned) > MAX_ANSWER_CHARS - 34:
        cleaned = cleaned[: MAX_ANSWER_CHARS - 34]
    return cleaned


def _story_value(story: Any, name: str, default: Any) -> Any:
    if isinstance(story, dict):
        return story.get(name, default)
    return getattr(story, name, default)


def _item_value(item: Any, name: str, default: Any = "") -> Any:
    if isinstance(item, dict):
        return item.get(name, default)
    if hasattr(item, "get"):
        return item.get(name, default)
    return getattr(item, name, default)
