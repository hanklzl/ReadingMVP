package com.littlemandarin.classics.shared.usecase

import com.littlemandarin.classics.shared.story.Story
import com.littlemandarin.classics.shared.story.StoryRepository

class GetStoryListUseCase(
    private val repository: StoryRepository,
) {
    suspend operator fun invoke(): List<Story> = repository.listStories()
}

class GetStoryUseCase(
    private val repository: StoryRepository,
) {
    suspend operator fun invoke(storyId: String): Story? = repository.getStory(storyId)
}

class ReadStoryUseCase(
    private val repository: StoryRepository,
) {
    suspend operator fun invoke(storyId: String): Story? = repository.getStory(storyId)
}
