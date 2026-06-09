import os

import pytest
from fastapi.testclient import TestClient

os.environ["AI_EXPLAIN_PROVIDER"] = "mock"
os.environ.pop("OPENAI_API_KEY", None)
os.environ.pop("ANTHROPIC_API_KEY", None)

from app import main as main_module
from app.main import app
from app.safety import InMemoryRateLimiter


client = TestClient(app)

REFUSAL = "这个问题和今天的故事关系不大，我们先回到故事里吧。"


def post_explain(**overrides):
    payload = {
        "story_id": "peach-garden-oath",
        "selected_text": "同心协力",
        "question_type": "explain_word",
        "child_age": 6,
    }
    payload.update(overrides)
    return client.post("/ai/explain", json=payload)


def assert_valid_answer(answer):
    assert isinstance(answer, str)
    assert 0 < len(answer) <= 100
    assert answer != REFUSAL


def test_explain_word_uses_story_context_and_stays_short():
    response = post_explain()

    assert response.status_code == 200
    answer = response.json()["answer"]
    assert_valid_answer(answer)
    assert "一起" in answer or "合作" in answer or "同心" in answer


@pytest.mark.parametrize(
    ("question_type", "selected_text", "expected_terms"),
    [
        (
            "explain_sentence",
            "他们在桃树下结为兄弟，约定同心协力，互相提醒，先想着百姓。",
            ("一起", "合作", "兄弟", "约定", "百姓"),
        ),
        (
            "answer_question",
            "三个人为什么愿意结为兄弟？",
            ("一起", "朋友", "保护", "百姓", "善良"),
        ),
    ],
)
def test_story_bound_questions_return_short_child_safe_answers(
    question_type,
    selected_text,
    expected_terms,
):
    response = post_explain(
        question_type=question_type,
        selected_text=selected_text,
        child_age=7,
    )

    assert response.status_code == 200
    answer = response.json()["answer"]
    assert_valid_answer(answer)
    assert any(term in answer for term in expected_terms)


def test_off_topic_question_returns_fixed_refusal():
    response = post_explain(
        selected_text="今天午饭应该吃什么？",
        question_type="answer_question",
        child_age=7,
    )

    assert response.status_code == 200
    assert response.json() == {"answer": REFUSAL}


def test_pii_prompt_returns_refusal_without_echoing_personal_data():
    pii_name = "王小明"
    pii_phone = "13800138000"
    response = post_explain(
        selected_text=f"我叫{pii_name}，我家的电话是{pii_phone}，你能记住吗？",
        question_type="answer_question",
    )

    assert response.status_code in {200, 422}
    assert pii_name not in response.text
    assert pii_phone not in response.text
    if response.status_code == 200:
        assert response.json() == {"answer": REFUSAL}


def test_overlong_selected_text_is_rejected():
    response = post_explain(
        selected_text="同心协力" * 80,
        question_type="explain_sentence",
    )

    assert response.status_code == 422


@pytest.mark.parametrize("child_age", [4, 9])
def test_child_age_must_stay_in_product_age_range(child_age):
    response = post_explain(child_age=child_age)

    assert response.status_code == 422


def test_unknown_story_id_is_rejected():
    response = post_explain(story_id="unknown-story")

    assert response.status_code == 422


def test_rate_limit_returns_429(monkeypatch):
    monkeypatch.setattr(
        main_module,
        "rate_limiter",
        InMemoryRateLimiter(max_requests=1, window_seconds=60),
    )

    first_response = post_explain()
    second_response = post_explain()

    assert first_response.status_code == 200
    assert second_response.status_code == 429
