from enum import StrEnum

from pydantic import BaseModel, ConfigDict, Field


class QuestionType(StrEnum):
    EXPLAIN_WORD = "explain_word"
    EXPLAIN_SENTENCE = "explain_sentence"
    ANSWER_QUESTION = "answer_question"


class ExplainRequest(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)

    story_id: str = Field(min_length=1, max_length=80, pattern=r"^[a-z0-9]+(-[a-z0-9]+)*$")
    selected_text: str = Field(min_length=1, max_length=120)
    question_type: QuestionType
    child_age: int = Field(ge=5, le=8)


class ExplainResponse(BaseModel):
    answer: str = Field(min_length=1, max_length=100)
