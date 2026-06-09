import pytest

from app.story_store import StoryNotFoundError, StoryStore


def test_story_store_loads_story_json_from_content_directory():
    story = StoryStore().get_story("peach-garden-oath")

    assert story.id == "peach-garden-oath"
    assert story.title_zh == "桃园三结义"
    assert any(item.get("word") == "同心协力" for item in story.vocab)


def test_story_store_rejects_unknown_story_ids():
    with pytest.raises(StoryNotFoundError):
        StoryStore().get_story("missing-story")
