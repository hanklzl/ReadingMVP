package com.littlemandarin.classics.shared.quiz

import com.littlemandarin.classics.shared.story.Story
import kotlinx.serialization.Serializable

/**
 * A question the child missed, with the correct answer and its child-friendly
 * explanation (the "why"), so remediation is diagnostic rather than just a score.
 */
data class MissedQuestion(
    val questionId: String,
    val prompt: String,
    val correctAnswer: String,
    val explanation: String,
)

/** A character/word to re-practise, surfaced from the story's vocabulary. */
data class ReviewWord(
    val word: String,
    val pinyin: String,
    val meaning: String,
)

/** A templated coaching line for the parent. Rendered/localized in the UI layer. */
@Serializable
enum class ReviewParentTip {
    ReadTogether,
    ReviewDueWords,
    PraiseAndOneWord,
}

/**
 * The next-day review pack: a short, templated diagnose → remediate → re-practise
 * bundle built from the just-finished quiz and the SRS due list. It is fully
 * deterministic and content-bounded — no open AI chat (AGENTS.md §7).
 */
data class ReviewPack(
    val storyId: String,
    val titleZh: String,
    val titleEn: String,
    val missedQuestions: List<MissedQuestion>,
    val reviewWords: List<ReviewWord>,
    val rereadSentence: String?,
    val parentTip: ReviewParentTip,
)

class MistakeRemediationUseCases {

    /**
     * Build a review pack from a completed [quizScore] for [story]. [dueWords] are the
     * SRS-due words (by surface form) so review prioritises what's actually due.
     */
    fun buildReviewPack(
        story: Story,
        quizScore: QuizScore,
        dueWords: Set<String> = emptySet(),
        maxReviewWords: Int = DefaultMaxReviewWords,
    ): ReviewPack {
        val questionsById = story.questions.associateBy { it.id }
        val missed = quizScore.results
            .filter { !it.isCorrect }
            .mapNotNull { result ->
                questionsById[result.questionId]?.let { question ->
                    MissedQuestion(
                        questionId = question.id,
                        prompt = question.prompt,
                        correctAnswer = question.answer,
                        explanation = question.explanation,
                    )
                }
            }

        // Prefer due vocab from this story, then fill with the rest, capped low.
        val dueFirst = story.vocab.filter { it.word in dueWords }
        val rest = story.vocab.filter { it.word !in dueWords }
        val reviewWords = (dueFirst + rest)
            .take(maxReviewWords.coerceAtLeast(0))
            .map { ReviewWord(it.word, it.pinyin, it.meaning) }

        val parentTip = when {
            quizScore.totalQuestions > 0 && quizScore.scorePercent < StruggleThresholdPercent ->
                ReviewParentTip.ReadTogether
            dueFirst.isNotEmpty() -> ReviewParentTip.ReviewDueWords
            else -> ReviewParentTip.PraiseAndOneWord
        }

        return ReviewPack(
            storyId = story.id,
            titleZh = story.titleZh,
            titleEn = story.titleEn,
            missedQuestions = missed,
            reviewWords = reviewWords,
            rereadSentence = rereadSentenceFor(story, missed),
            parentTip = parentTip,
        )
    }

    /**
     * Choose one short sentence to re-read: prefer one containing a missed answer's
     * keyword (re-reading the evidence), else the story's first sentence.
     */
    private fun rereadSentenceFor(story: Story, missed: List<MissedQuestion>): String? {
        val sentences = story.paragraphs
            .flatMap { splitSentences(it.text) }
            .filter { it.isNotBlank() }
        if (sentences.isEmpty()) return null

        for (missedQuestion in missed) {
            val keyword = missedQuestion.correctAnswer.trim()
            if (keyword.isNotBlank()) {
                sentences.firstOrNull { it.contains(keyword) }?.let { return it }
            }
        }
        return sentences.first()
    }

    private fun splitSentences(text: String): List<String> {
        val sentences = mutableListOf<String>()
        val builder = StringBuilder()
        for (char in text) {
            builder.append(char)
            if (char == '。' || char == '！' || char == '？') {
                sentences.add(builder.toString())
                builder.clear()
            }
        }
        if (builder.isNotBlank()) sentences.add(builder.toString())
        return sentences
    }

    private companion object {
        const val DefaultMaxReviewWords = 3
        const val StruggleThresholdPercent = 60.0
    }
}
