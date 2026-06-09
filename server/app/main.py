from fastapi import FastAPI, HTTPException, Request, status

from app.models import ExplainRequest, ExplainResponse
from app.providers import get_provider
from app.safety import (
    REFUSAL_MESSAGE,
    InMemoryRateLimiter,
    contains_pii,
    sanitize_answer,
    validate_story_scope,
)
from app.story_store import StoryContext, StoryNotFoundError, StoryStore


app = FastAPI(title="Little Mandarin Classics AI Explain API")
story_store = StoryStore()
rate_limiter = InMemoryRateLimiter()


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/ai/explain", response_model=ExplainResponse)
def explain(request_body: ExplainRequest, request: Request) -> ExplainResponse:
    rate_key = request.client.host if request.client else "local"
    if not rate_limiter.check(rate_key):
        raise HTTPException(status_code=status.HTTP_429_TOO_MANY_REQUESTS, detail="Too many requests.")

    try:
        story = story_store.load(request_body.story_id)
    except StoryNotFoundError as exc:
        raise HTTPException(
            status_code=422,
            detail="Story not found.",
        ) from exc

    if contains_pii(request_body.selected_text) or not validate_story_scope(request_body.selected_text, story):
        return ExplainResponse(answer=REFUSAL_MESSAGE)

    prompt = build_prompt(request_body, story)
    try:
        provider = get_provider()
        answer = provider.explain(request_body, story, prompt)
    except RuntimeError as exc:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail=str(exc)) from exc

    return ExplainResponse(answer=sanitize_answer(answer))


def build_prompt(request_body: ExplainRequest, story: StoryContext) -> str:
    vocab_lines = "\n".join(
        f"- {item.get('word', '')}: {item.get('meaning', '')}; example: {item.get('example', '')}"
        for item in story.vocab
    )
    question_lines = "\n".join(
        f"- {item.get('prompt', '')} answer: {item.get('answer', '')}; explanation: {item.get('explanation', '')}"
        for item in story.questions
    )
    return (
        "你是小小中文经典的受控 AI 陪读助手。只围绕当前故事解释，不做开放聊天。\n"
        f"如果输入和故事无关，必须只回答：{REFUSAL_MESSAGE}\n"
        "不要询问、记录或复述儿童真实姓名、电话、住址、学校等个人信息。\n"
        "不要生成恐怖、血腥、成人内容或暴力细节。回答必须适合 5-8 岁儿童，温和，100 个汉字以内。\n"
        f"孩子年龄：{request_body.child_age}\n"
        f"问题类型：{request_body.question_type.value}\n"
        f"选中文本/问题：{request_body.selected_text}\n"
        f"故事：{story.title_zh} / {story.title_en}\n"
        f"正文：{story.body}\n"
        f"生字：\n{vocab_lines}\n"
        f"理解题：\n{question_lines}\n"
        f"复述提示：{story.retell_prompt}\n"
    )
