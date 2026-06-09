package com.littlemandarin.classics.shared.story

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Story(
    val id: String,
    @SerialName("title_zh")
    val titleZh: String,
    @SerialName("title_en")
    val titleEn: String,
    val level: Int,
    @SerialName("age_range")
    val ageRange: String,
    @SerialName("source_note")
    val sourceNote: String,
    @SerialName("source_url")
    val sourceUrl: String? = null,
    @SerialName("cover_image")
    val coverImage: String? = null,
    val paragraphs: List<Paragraph>,
    val vocab: List<Vocab>,
    val questions: List<Question>,
    @SerialName("retell_prompt")
    val retellPrompt: String,
)

@Serializable
data class Paragraph(
    val text: String,
    val pinyin: String,
)

@Serializable
data class Vocab(
    val word: String,
    val pinyin: String,
    val meaning: String,
    val example: String? = null,
)

@Serializable
data class Question(
    val id: String,
    val type: String,
    val prompt: String,
    val options: List<String>,
    val answer: String,
    val explanation: String,
)
