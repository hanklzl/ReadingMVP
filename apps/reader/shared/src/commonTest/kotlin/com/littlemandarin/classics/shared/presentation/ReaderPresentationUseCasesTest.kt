package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.analytics.AnalyticsContext
import com.littlemandarin.classics.shared.analytics.AnalyticsEventName
import com.littlemandarin.classics.shared.analytics.AnalyticsPlatform
import com.littlemandarin.classics.shared.analytics.InMemoryAnalyticsService
import com.littlemandarin.classics.shared.progress.CompletionRecord
import com.littlemandarin.classics.shared.progress.ParentProgressReport
import com.littlemandarin.classics.shared.progress.ProgressStats
import com.littlemandarin.classics.shared.quiz.QuizScore
import com.littlemandarin.classics.shared.service.AiExplanationResponse
import com.littlemandarin.classics.shared.service.AiMessageKeys
import com.littlemandarin.classics.shared.service.AiQuestionTypes
import com.littlemandarin.classics.shared.story.Paragraph
import com.littlemandarin.classics.shared.story.Question
import com.littlemandarin.classics.shared.story.Story
import com.littlemandarin.classics.shared.story.StoryAudioManifest
import com.littlemandarin.classics.shared.story.StoryAudioSegment
import com.littlemandarin.classics.shared.story.Vocab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ReaderPresentationUseCasesTest {
    @Test
    fun readingSessionClampsSavedIndexAndReportsNextTransition() {
        val story = sampleStory(paragraphCount = 3)
        val reducer = ReadingSessionReducer()

        val initial = reducer.initialState(story, savedParagraphIndex = 99)
        val previous = reducer.previous(story, initial)
        val next = reducer.next(story, previous)

        assertEquals(2, initial.paragraphIndex)
        assertEquals(3, initial.paragraphCount)
        assertTrue(initial.isLastParagraph)
        assertEquals(1.0, initial.progressFraction)
        assertEquals(1, previous.paragraphIndex)
        assertEquals("${sampleParagraphText()}1", previous.currentParagraph?.text)
        assertFalse(next.shouldOpenVocabulary)
        assertEquals(2, next.state.paragraphIndex)
        assertEquals("${sampleParagraphText()}2", next.state.currentParagraph?.text)
        assertTrue(reducer.next(story, next.state).shouldOpenVocabulary)
    }

    @Test
    fun readingSessionAdvancesBySentenceAndStopsAtVocabularyGate() {
        val story = sampleStory(paragraphCount = 2).copy(
            paragraphs = listOf(
                Paragraph(
                    text = "桃园开花。三人说：好！",
                    pinyin = "tao yuan kai hua. san ren shuo hao.",
                ),
                Paragraph(
                    text = "大家点头……",
                    pinyin = "da jia dian tou.",
                ),
            ),
        )
        val manifest = StoryAudioManifest(
            storyId = story.id,
            segments = listOf(
                StoryAudioSegment(
                    paragraphIndex = 0,
                    sentenceIndex = 0,
                    text = "桃园开花。",
                    resourcePath = "stories/sample/audio/p1_s1.wav",
                    durationMillis = 2100L,
                ),
            ),
        )
        val reducer = ReadingSessionReducer()

        val initial = reducer.initialState(story, savedParagraphIndex = 0, audioManifest = manifest)
        val playing = reducer.start(initial)
        val paused = reducer.pause(playing)
        val stopped = reducer.stop(paused)
        val secondSentence = reducer.nextSentence(story, playing, audioManifest = manifest)
        val nextParagraph = reducer.nextSentence(story, secondSentence.state, audioManifest = manifest)
        val vocabularyGate = reducer.nextSentence(story, nextParagraph.state, audioManifest = manifest)

        assertEquals(listOf("桃园开花。", "三人说：好！"), initial.segments.map { it.text })
        assertEquals(0, initial.sentenceIndex)
        assertEquals("桃园开花。", initial.currentSentence?.text)
        assertTrue(initial.hasGeneratedAudio)
        assertEquals("stories/sample/audio/p1_s1.wav", initial.currentAudioResourcePath)

        assertEquals(ReadingPlaybackStatus.Playing, playing.playbackStatus)
        assertEquals(ReadingPlaybackStatus.Paused, paused.playbackStatus)
        assertEquals(ReadingPlaybackStatus.Stopped, stopped.playbackStatus)

        assertFalse(secondSentence.shouldOpenVocabulary)
        assertEquals(0, secondSentence.state.paragraphIndex)
        assertEquals(1, secondSentence.state.sentenceIndex)
        assertEquals("三人说：好！", secondSentence.state.currentSentence?.text)
        assertFalse(secondSentence.state.hasGeneratedAudio)

        assertEquals(1, nextParagraph.state.paragraphIndex)
        assertEquals(0, nextParagraph.state.sentenceIndex)
        assertEquals("大家点头……", nextParagraph.state.currentSentence?.text)

        assertTrue(vocabularyGate.shouldOpenVocabulary)
        assertEquals(ReadingPlaybackStatus.Stopped, vocabularyGate.state.playbackStatus)
        assertEquals(1, vocabularyGate.state.paragraphIndex)
        assertEquals(0, vocabularyGate.state.sentenceIndex)
    }

    @Test
    fun quizSessionTracksSelectionSubmissionAdvanceAndCompletionRecord() {
        val story = sampleStory()
        val reducer = QuizSessionReducer()
        var state = reducer.initialState(story)

        state = reducer.selectAnswer(story, state, "A")
        state = reducer.submitOrAdvance(story, state)

        assertEquals("A", reducer.currentSelectedAnswer(story, state))
        assertTrue(reducer.isCurrentQuestionSubmitted(story, state))
        assertEquals(1, reducer.score(story, state).correctCount)

        state = reducer.submitOrAdvance(story, state)
        state = reducer.selectAnswer(story, state, "B")
        state = reducer.submitOrAdvance(story, state)
        state = reducer.submitOrAdvance(story, state)
        state = reducer.selectAnswer(story, state, "B")
        state = reducer.submitOrAdvance(story, state)
        state = reducer.submitOrAdvance(story, state)

        assertTrue(state.isComplete)
        assertEquals(2, reducer.score(story, state).correctCount)
        assertEquals(
            CompletionRecord(
                storyId = story.id,
                completedAtEpochMillis = 1_800_000_000_000L,
                vocabCount = story.vocab.size,
                correctCount = 2,
                questionCount = 3,
            ),
            reducer.completionRecord(story, state, nowEpochMillis = 1_800_000_000_000L),
        )
    }

    @Test
    fun storyPresentationSelectsTodayStoriesAndComputesPartialProgress() {
        val useCases = StoryPresentationUseCases()
        val stories = listOf(sampleStory(id = "first"), sampleStory(id = "second"), sampleStory(id = "third"))

        val catalogSelection = useCases.selectTodayStories(
            stories = stories,
            completedStoryIds = setOf("first"),
            policy = TodayStorySelectionPolicy.CatalogFirst,
        )
        val incompleteSelection = useCases.selectTodayStories(
            stories = stories,
            completedStoryIds = setOf("first"),
            policy = TodayStorySelectionPolicy.FirstIncomplete,
        )

        assertEquals("first", catalogSelection.todayStory?.id)
        assertEquals("second", catalogSelection.upNextStory?.id)
        assertEquals("second", incompleteSelection.todayStory?.id)
        assertEquals("third", incompleteSelection.upNextStory?.id)

        val partial = useCases.storyProgress(
            story = stories.first(),
            completedStoryIds = emptySet(),
            savedParagraphIndex = 1,
        )
        val complete = useCases.storyProgress(
            story = stories.first(),
            completedStoryIds = setOf("first"),
            savedParagraphIndex = 0,
        )

        assertEquals(StoryProgressStatus.InProgress, partial.status)
        assertEquals(2.0 / 3.0, partial.fraction, 0.0001)
        assertEquals(StoryProgressStatus.Completed, complete.status)
        assertEquals(1.0, complete.fraction)
        assertFalse(useCases.canOpenQuiz(stories.first(), completedStoryIds = emptySet()))
        assertTrue(useCases.canOpenQuiz(stories.first(), completedStoryIds = setOf("first")))
    }

    @Test
    fun parentReportSummaryCountsDistinctReadingDaysInsideWeekWindow() {
        val now = 1_800_000_000_000L
        val oneDay = 24L * 60L * 60L * 1_000L
        val report = ParentProgressReport(
            storiesCompletedThisWeek = 2,
            vocabLearnedThisWeek = 11,
            averageCorrectPercent = 83.33,
            recentCompletions = listOf(
                CompletionRecord("first", now - oneDay, 6, 3, 3),
                CompletionRecord("second", now - oneDay + 10_000L, 5, 2, 3),
                CompletionRecord("old", now - 9L * oneDay, 5, 1, 3),
            ),
        )
        val stats = ProgressStats(
            completedCount = 3,
            vocabLearnedCount = 16,
            correctCount = 6,
            questionCount = 9,
            averageCorrectPercent = 66.67,
        )

        val summary = BuildParentReportSummaryUseCase().invoke(
            report = report,
            stats = stats,
            nowEpochMillis = now,
        )

        assertEquals(1, summary.readingDaysThisWeek)
        assertEquals(2, summary.storiesCompletedThisWeek)
        assertEquals(11, summary.vocabLearnedThisWeek)
        assertEquals(6, summary.correctCount)
        assertEquals(9, summary.questionCount)
    }

    @Test
    fun aiRequestBuilderUsesCurrentParagraphAndSharedLimits() {
        val story = sampleStory(
            paragraphCount = 1,
            paragraphText = "甲".repeat(140),
        )
        val builder = BuildAiExplanationRequestUseCase()

        val request = assertNotNull(builder.forParagraph(story, paragraphIndex = 10))
        val blankRequest = builder.forParagraph(story.copy(paragraphs = listOf(Paragraph(" ", " "))), 0)
        val answer = AiExplanationResponse(
            answer = "ignored",
            messageKey = AiMessageKeys.ExplanationStub,
        ).toLimitedDisplayText(
            stubText = "乙".repeat(120),
            outOfScopeText = "out",
        )

        assertEquals(story.id, request.storyId)
        assertEquals(120, request.selectedText.length)
        assertEquals(AiQuestionTypes.ExplainSentence, request.questionType)
        assertNull(blankRequest)
        assertEquals(100, answer.length)
    }

    @Test
    fun settingsReducerNormalizesRawValuesAndMockBackendSentinel() = runTest {
        val reducer = ReaderSettingsReducer()

        val defaultState = reducer.defaultState()
        val changed = reducer.aiBackendBaseUrlChanged(defaultState, " local/mock ")

        assertEquals(ReaderLanguage.English, ReaderLanguage.fromTag("missing"))
        assertEquals(ReadingTextSize.Medium, ReadingTextSize.fromPrefValue("missing"))
        assertEquals(ReaderLanguage.ChineseSimplified, reducer.languageChanged(defaultState, ReaderLanguage.ChineseSimplified).language)
        assertTrue(changed.isMockAiBackend)
        assertEquals("local/mock", changed.aiBackendBaseUrl)

        val settingsService = InMemoryReaderSettingsService(
            initialSettings = ReaderSettings(aiBackendBaseUrl = "local/mock"),
        )
        settingsService.setAiBackendBaseUrl("https://example.test")
        settingsService.setAiBackendBaseUrl(" ")
        assertEquals("local/mock", settingsService.read().aiBackendBaseUrl)
    }

    @Test
    fun analyticsEventFactoryBuildsSchemaValidSharedPayloads() = runTest {
        val analytics = InMemoryAnalyticsService(
            context = AnalyticsContext(
                anonymousInstallId = "install",
                sessionId = "session",
                platform = AnalyticsPlatform.Android,
                appVersion = "1.0",
                uiLocale = "en",
            ),
            runtime = FixedAnalyticsRuntime(),
        )
        val story = sampleStory(id = "first")

        val storyOpen = ReaderAnalyticsEvents.storyOpen(
            story = story,
            storyOrder = 1,
            openSource = "today",
            previousStoryStatus = StoryProgressStatus.NotStarted,
        )
        val quizComplete = ReaderAnalyticsEvents.quizComplete(
            story = story,
            score = QuizScore(
                correctCount = 2,
                totalQuestions = 3,
                scorePercent = 66.67,
                results = emptyList(),
            ),
        )
        val storyComplete = ReaderAnalyticsEvents.storyComplete(
            story = story,
            storyOrder = 1,
            quizCompleted = true,
        )
        val aiExplain = ReaderAnalyticsEvents.aiExplainRequest(
            storyId = story.id,
            requestType = AiQuestionTypes.ExplainSentence,
            safetyOutcome = "allowed",
            targetType = "paragraph",
        )
        val parentReportOpen = ReaderAnalyticsEvents.parentReportOpen("settings")
        val pinyinToggle = ReaderAnalyticsEvents.pinyinToggle(
            storyId = story.id,
            enabled = true,
            surface = "reading",
            paragraphIndex = 2,
        )
        val paragraphAudioPlay = ReaderAnalyticsEvents.paragraphAudioPlay(
            storyId = story.id,
            paragraphIndex = 0,
            audioSource = "generated",
            sentenceIndex = 1,
        )
        val vocabOpen = ReaderAnalyticsEvents.vocabOpen(story, vocabIndex = 1, openSource = "story")

        val event = analytics.track(storyOpen.eventName, storyOpen.properties)

        assertEquals(AnalyticsEventName.StoryOpen, event.eventName)
        assertEquals("first", event.properties.getValue("story_id").toString().trim('"'))
        assertEquals(AnalyticsEventName.QuizComplete, quizComplete.eventName)
        assertEquals("2", quizComplete.properties.getValue("correct_count").toString())
        assertEquals(AnalyticsEventName.StoryComplete, storyComplete.eventName)
        assertEquals("true", storyComplete.properties.getValue("quiz_completed").toString())
        assertEquals(AnalyticsEventName.AiExplainRequest, aiExplain.eventName)
        assertEquals("allowed", aiExplain.properties.getValue("safety_outcome").toString().trim('"'))
        assertEquals(AnalyticsEventName.ParentReportOpen, parentReportOpen.eventName)
        assertEquals("settings", parentReportOpen.properties.getValue("entry_point").toString().trim('"'))
        assertEquals(AnalyticsEventName.PinyinToggle, pinyinToggle.eventName)
        assertEquals("2", pinyinToggle.properties.getValue("paragraph_index").toString())
        assertEquals(AnalyticsEventName.ParagraphAudioPlay, paragraphAudioPlay.eventName)
        assertEquals("1", paragraphAudioPlay.properties.getValue("sentence_index").toString())
        assertEquals(AnalyticsEventName.VocabOpen, vocabOpen.eventName)
        assertEquals("first:2", vocabOpen.properties.getValue("vocab_id").toString().trim('"'))
    }

    @Test
    fun feedbackSubmissionBuilderTrimsInputAndReportsSubmitEligibility() {
        val builder = BuildFeedbackSubmissionUseCase()

        val submission = builder.invoke(
            satisfaction = com.littlemandarin.classics.shared.feedback.FeedbackSatisfaction.Satisfied,
            childAgeBand = com.littlemandarin.classics.shared.feedback.FeedbackChildAgeBand.Age5To6,
            issueType = com.littlemandarin.classics.shared.feedback.FeedbackIssueType.AudioIssue,
            suggestion = "  Please slow down audio.  ",
            parentContact = "  parent@example.com  ",
        )

        assertFalse(builder.canSubmit(" "))
        assertTrue(builder.canSubmit("Good app"))
        assertEquals("Please slow down audio.", submission.suggestion)
        assertEquals("parent@example.com", submission.parentContact)
        assertEquals(com.littlemandarin.classics.shared.feedback.FeedbackSatisfaction.Neutral, FeedbackPresentationOptions.satisfaction[1].value)
        assertEquals(com.littlemandarin.classics.shared.feedback.FeedbackIssueType.Other, FeedbackPresentationOptions.issueTypes[3].value)
        assertEquals("progress", FeedbackPresentationOptions.issueTypes[3].id)
    }
}

private class FixedAnalyticsRuntime : com.littlemandarin.classics.shared.analytics.AnalyticsRuntime {
    override fun newUuid(): String = "event"

    override fun nowUtcIsoString(): String = "2026-06-10T00:00:00Z"

    override fun platform(): AnalyticsPlatform = AnalyticsPlatform.Android
}

private fun sampleStory(
    id: String = "sample",
    paragraphCount: Int = 3,
    paragraphText: String = "桃园里，三个人说好一起做正直勇敢的事。",
): Story = Story(
    id = id,
    titleZh = "桃园结义",
    titleEn = "Oath in the Peach Garden",
    level = 1,
    ageRange = "5-8",
    sourceNote = "public domain",
    paragraphs = List(paragraphCount) { index ->
        Paragraph(
            text = if (index == 0) paragraphText else "$paragraphText$index",
            pinyin = "tao yuan",
        )
    },
    vocab = listOf(
        Vocab("结义", "jie yi", "to become sworn friends", "他们结义。"),
        Vocab("勇敢", "yong gan", "brave", "他很勇敢。"),
    ),
    questions = listOf(
        Question("q1", "single_choice", "谁说好一起做事？", listOf("A", "B", "C"), "A", "他们要互相帮助。"),
        Question("q2", "single_choice", "他们想做什么？", listOf("A", "B", "C"), "B", "故事说他们想做正直的事。"),
        Question("q3", "single_choice", "这个故事强调什么？", listOf("A", "B", "C"), "C", "朋友之间要合作。"),
    ),
    retellPrompt = "说说三个人为什么要合作。",
)

private fun sampleParagraphText(): String = "桃园里，三个人说好一起做正直勇敢的事。"
