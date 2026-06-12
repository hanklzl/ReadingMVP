package com.littlemandarin.classics.shared.story

data class StoryResourceEntry(
    val id: String,
    val path: String,
)

object StoryResourceCatalog {
    val entries: List<StoryResourceEntry> = listOf(
        StoryResourceEntry(
            id = "peach-garden-oath",
            path = "stories/peach-garden-oath/story.json",
        ),
        StoryResourceEntry(
            id = "three-heroes-vs-lubu",
            path = "stories/three-heroes-vs-lubu/story.json",
        ),
        StoryResourceEntry(
            id = "quench-thirst-plums",
            path = "stories/quench-thirst-plums/story.json",
        ),
        StoryResourceEntry(
            id = "green-plum-heroes",
            path = "stories/green-plum-heroes/story.json",
        ),
        StoryResourceEntry(
            id = "thousand-mile-loyalty",
            path = "stories/thousand-mile-loyalty/story.json",
        ),
        StoryResourceEntry(
            id = "the-leap-of-faith",
            path = "stories/the-leap-of-faith/story.json",
        ),
        StoryResourceEntry(
            id = "three-visits-cottage",
            path = "stories/three-visits-cottage/story.json",
        ),
        StoryResourceEntry(
            id = "zhaoyun-changban",
            path = "stories/zhaoyun-changban/story.json",
        ),
        StoryResourceEntry(
            id = "debate-scholars",
            path = "stories/debate-scholars/story.json",
        ),
        StoryResourceEntry(
            id = "borrow-arrows-boats",
            path = "stories/borrow-arrows-boats/story.json",
        ),
        StoryResourceEntry(
            id = "borrow-east-wind",
            path = "stories/borrow-east-wind/story.json",
        ),
        StoryResourceEntry(
            id = "red-cliffs",
            path = "stories/red-cliffs/story.json",
        ),
        StoryResourceEntry(
            id = "huarong-path",
            path = "stories/huarong-path/story.json",
        ),
        StoryResourceEntry(
            id = "single-blade-meeting",
            path = "stories/single-blade-meeting/story.json",
        ),
        StoryResourceEntry(
            id = "calming-five-routes",
            path = "stories/calming-five-routes/story.json",
        ),
        StoryResourceEntry(
            id = "seven-captures",
            path = "stories/seven-captures/story.json",
        ),
        StoryResourceEntry(
            id = "careful-letter-chushi",
            path = "stories/careful-letter-chushi/story.json",
        ),
        StoryResourceEntry(
            id = "street-pavilion-lesson",
            path = "stories/street-pavilion-lesson/story.json",
        ),
        StoryResourceEntry(
            id = "empty-fort",
            path = "stories/empty-fort/story.json",
        ),
        StoryResourceEntry(
            id = "wooden-ox-flowing-horse",
            path = "stories/wooden-ox-flowing-horse/story.json",
        ),
    )
}
