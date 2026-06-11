package com.littlemandarin.classics.shared.story

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class StoryContractTest {
    @Test
    fun decodeStoryParsesRealPeachGardenJson() {
        val story = StoryJson.decodeStory(peachGardenOathJson)

        assertEquals("peach-garden-oath", story.id)
        assertEquals("桃园三结义", story.titleZh)
        assertEquals("The Oath of the Peach Garden", story.titleEn)
        assertCompleteStoryModel(story)
    }

    @Test
    fun decodeStoryParsesParagraphPinyinCells() {
        val story = StoryJson.decodeStory(storyJsonWithPinyinCells)

        val paragraph = story.paragraphs.single()

        assertEquals("桃园，好", paragraph.text)
        assertEquals(listOf("桃", "园", "，", "好"), paragraph.cells.map { it.c })
        assertEquals(listOf("táo", "yuán", "", "hǎo"), paragraph.cells.map { it.p })
        assertPinyinCellsMatchParagraphText(
            storyId = story.id,
            paragraphIndex = 0,
            paragraph = paragraph,
        )
    }

    @Test
    fun repositoryListsAllTenStoriesAndFindsPeachGardenOath() = runTest {
        val repository = DefaultStoryRepository()

        val stories = repository.listStories()

        assertEquals(15, stories.size)
        assertEquals(expectedStoryIds, stories.map { it.id })
        stories.forEach(::assertCompleteStoryModel)

        val peachGarden = assertNotNull(repository.getStory("peach-garden-oath"))
        assertEquals("桃园三结义", peachGarden.titleZh)
        assertEquals("The Oath of the Peach Garden", peachGarden.titleEn)
        assertEquals("说说刘备、关羽、张飞为什么愿意结为兄弟。", peachGarden.retellPrompt)
    }

    @Test
    fun repositoryStoriesProvidePinyinCellsForEveryParagraph() = runTest {
        val repository = DefaultStoryRepository()

        val stories = repository.listStories()

        assertEquals(15, stories.size)
        stories.forEach { story ->
            story.paragraphs.forEachIndexed { index, paragraph ->
                assertPinyinCellsMatchParagraphText(
                    storyId = story.id,
                    paragraphIndex = index,
                    paragraph = paragraph,
                )
            }
        }
    }
}

private fun assertCompleteStoryModel(story: Story) {
    assertTrue(story.id.isNotBlank(), "story id should be present")
    assertTrue(story.titleZh.isNotBlank(), "Chinese title should be present")
    assertTrue(story.titleEn.isNotBlank(), "English title should be present")
    assertTrue(story.paragraphs.isNotEmpty(), "paragraphs should be present")
    story.paragraphs.forEach { paragraph ->
        assertTrue(paragraph.text.isNotBlank(), "paragraph text should be present")
        assertTrue(paragraph.pinyin.isNotBlank(), "paragraph pinyin should be present")
    }

    assertTrue(story.vocab.size in 5..8, "vocab count should match content contract")
    story.vocab.forEach { vocab ->
        assertTrue(vocab.word.isNotBlank(), "vocab word should be present")
        assertTrue(vocab.pinyin.isNotBlank(), "vocab pinyin should be present")
        assertTrue(vocab.meaning.isNotBlank(), "vocab English meaning should be present")
    }

    assertEquals(3, story.questions.size)
    story.questions.forEach { question ->
        assertEquals("single_choice", question.type)
        assertTrue(question.id.isNotBlank(), "question id should be present")
        assertTrue(question.prompt.isNotBlank(), "question prompt should be present")
        assertTrue(question.options.size in 2..4, "question options should match content contract")
        assertTrue(question.answer in question.options, "question answer must be one of the options")
        assertTrue(question.explanation.isNotBlank(), "question explanation should be present")
    }

    assertTrue(story.retellPrompt.isNotBlank(), "retell prompt should be present")
}

private fun assertPinyinCellsMatchParagraphText(
    storyId: String,
    paragraphIndex: Int,
    paragraph: Paragraph,
) {
    val context = "$storyId paragraph ${paragraphIndex + 1}"
    assertEquals(
        expected = paragraph.text.length,
        actual = paragraph.cells.size,
        message = "$context should have one pinyin cell per text character",
    )
    assertEquals(
        expected = paragraph.text,
        actual = paragraph.cells.joinToString(separator = "") { it.c },
        message = "$context cells should reconstruct paragraph text",
    )

    paragraph.cells.forEachIndexed { cellIndex, cell ->
        val cellContext = "$context cell ${cellIndex + 1}"
        assertEquals(
            expected = 1,
            actual = cell.c.length,
            message = "$cellContext should contain exactly one character",
        )

        if (isHanCharacter(cell.c[0])) {
            assertTrue(cell.p.isNotBlank(), "$cellContext Han character should have pinyin")
        } else {
            assertTrue(cell.p.isEmpty(), "$cellContext non-Han character should not have pinyin")
        }
    }
}

private fun isHanCharacter(character: Char): Boolean =
    character in '\u3400'..'\u4DBF' ||
        character in '\u4E00'..'\u9FFF' ||
        character in '\uF900'..'\uFAFF'

private val expectedStoryIds = listOf(
    "peach-garden-oath",
    "three-heroes-vs-lubu",
    "quench-thirst-plums",
    "green-plum-heroes",
    "thousand-mile-loyalty",
    "three-visits-cottage",
    "zhaoyun-changban",
    "debate-scholars",
    "borrow-arrows-boats",
    "borrow-east-wind",
    "red-cliffs",
    "huarong-path",
    "seven-captures",
    "empty-fort",
    "wooden-ox-flowing-horse",
)

private val storyJsonWithPinyinCells = """
{
  "id": "cells-inline",
  "title_zh": "桃园",
  "title_en": "Peach Garden",
  "level": 1,
  "age_range": "5-8",
  "source_note": "Inline test fixture.",
  "paragraphs": [
    {
      "text": "桃园，好",
      "pinyin": "táo yuán， hǎo",
      "cells": [
        {
          "c": "桃",
          "p": "táo"
        },
        {
          "c": "园",
          "p": "yuán"
        },
        {
          "c": "，",
          "p": ""
        },
        {
          "c": "好",
          "p": "hǎo"
        }
      ]
    }
  ],
  "vocab": [
    {
      "word": "桃园",
      "pinyin": "táo yuán",
      "meaning": "peach garden"
    },
    {
      "word": "朋友",
      "pinyin": "péng yǒu",
      "meaning": "friend"
    },
    {
      "word": "约定",
      "pinyin": "yuē dìng",
      "meaning": "promise"
    },
    {
      "word": "帮助",
      "pinyin": "bāng zhù",
      "meaning": "help"
    },
    {
      "word": "善良",
      "pinyin": "shàn liáng",
      "meaning": "kindness"
    }
  ],
  "questions": [
    {
      "id": "q1",
      "type": "single_choice",
      "prompt": "故事里提到哪里？",
      "options": [
        "桃园",
        "雪山"
      ],
      "answer": "桃园",
      "explanation": "正文写到了桃园。"
    },
    {
      "id": "q2",
      "type": "single_choice",
      "prompt": "桃园怎么样？",
      "options": [
        "很好",
        "很远"
      ],
      "answer": "很好",
      "explanation": "正文说桃园好。"
    },
    {
      "id": "q3",
      "type": "single_choice",
      "prompt": "这个测试关注什么？",
      "options": [
        "逐字拼音",
        "开放聊天"
      ],
      "answer": "逐字拼音",
      "explanation": "这个 JSON 包含逐字拼音 cells。"
    }
  ],
  "retell_prompt": "说说桃园里发生了什么。"
}
""".trimIndent()

private val peachGardenOathJson = """
{
  "id": "peach-garden-oath",
  "title_zh": "桃园三结义",
  "title_en": "The Oath of the Peach Garden",
  "level": 1,
  "age_range": "5-8",
  "source_note": "Based on public-domain 《三国演义》, rewritten for children.",
  "source_url": "https://zh.wikisource.org/wiki/三國演義/第001回",
  "cover_image": "stories/peach-garden-oath/cover.png",
  "paragraphs": [
    {
      "text": "很久以前，涿县贴出榜文，盼望有人一起保护乡里。刘备读完榜文，心里想着百姓过安稳日子，不由得长叹。他明白，一个人的力量有限，若能找到志同道合的朋友，就能多做一点好事。",
      "pinyin": "hěn jiǔ yǐ qián， zhuō xiàn tiē chū bǎng wén， pàn wàng yǒu rén yì qǐ bǎo hù xiāng lǐ。 liú bèi dú wán bǎng wén， xīn lǐ xiǎng zhe bǎi xìng guò ān wěn rì zi， bù yóu de cháng tàn。 tā míng bái， yí gè rén de lì liàng yǒu xiàn， ruò néng zhǎo dào zhì tóng dào hé de péng yǒu， jiù néng duō zuò yì diǎn hǎo shì。"
    },
    {
      "text": "张飞听见叹声，走来问原因。刘备说自己力量小，却想做有益的事。张飞很赞成，愿意拿出家中积蓄，招呼乡勇。他说，家里有粮有地方，可以先让愿意帮忙的人聚在一起。",
      "pinyin": "zhāng fēi tīng jiàn tàn shēng， zǒu lái wèn yuán yīn。 liú bèi shuō zì jǐ lì liàng xiǎo， què xiǎng zuò yǒu yì de shì。 zhāng fēi hěn zàn chéng， yuàn yì ná chū jiā zhōng jī xù， zhāo hū xiāng yǒng。 tā shuō， jiā lǐ yǒu liáng yǒu dì fāng， kě yǐ xiān ràng yuàn yì bāng máng de rén jù zài yì qǐ。"
    },
    {
      "text": "这时关羽也来到店里。他听见二人的心愿，觉得他们正直可靠，便坐下一起商量。三个人越谈越投合，都想扶危助困。关羽说，做大事先要守信用，也要懂得照顾弱小的人。",
      "pinyin": "zhè shí guān yǔ yě lái dào diàn lǐ。 tā tīng jiàn èr rén de xīn yuàn， jué de tā men zhèng zhí kě kào， biàn zuò xià yì qǐ shāng liáng。 sān gè rén yuè tán yuè tóu hé， dōu xiǎng fú wēi zhù kùn。 guān yǔ shuō， zuò dà shì xiān yào shǒu xìn yòng， yě yào dǒng de zhào gù ruò xiǎo de rén。"
    },
    {
      "text": "张飞家后有一片桃园，桃花开得正好。第二天，他们在桃树下结为兄弟，约定同心协力，互相提醒，先想着百姓。桃花落在衣袖上，像给这个约定盖上温柔的印记。",
      "pinyin": "zhāng fēi jiā hòu yǒu yī piàn táo yuán， táo huā kāi dé zhèng hǎo。 dì èr tiān， tā men zài táo shù xià jié wéi xiōng dì， yuē dìng tóng xīn xié lì， hù xiāng tí xǐng， xiān xiǎng zhe bǎi xìng。 táo huā luò zài yī xiù shàng， xiàng gěi zhè ge yuē dìng gài shàng wēn róu de yìn jì。"
    },
    {
      "text": "从此，刘备、关羽、张飞一起练习、准备、照看乡亲。遇到有人搬东西，他们去帮忙；听见老人担心，他们耐心安慰。桃园里的誓言像春风一样，告诉大家：真正的勇气，是和朋友一起守护善良。",
      "pinyin": "cóng cǐ， liú bèi、 guān yǔ、 zhāng fēi yì qǐ liàn xí、 zhǔn bèi、 zhào kàn xiāng qīn。 yù dào yǒu rén bān dōng xī， tā men qù bāng máng； tīng jiàn lǎo rén dān xīn， tā men nài xīn ān wèi。 táo yuán lǐ de shì yán xiàng chūn fēng yī yàng， gào sù dà jiā： zhēn zhèng de yǒng qì， shì hé péng yǒu yì qǐ shǒu hù shàn liáng。"
    }
  ],
  "vocab": [
    {
      "word": "榜文",
      "pinyin": "bǎng wén",
      "meaning": "public notice",
      "example": "涿县贴出榜文。"
    },
    {
      "word": "乡里",
      "pinyin": "xiāng lǐ",
      "meaning": "hometown area",
      "example": "大家一起保护乡里。"
    },
    {
      "word": "积蓄",
      "pinyin": "jī xù",
      "meaning": "savings",
      "example": "张飞愿意拿出积蓄。"
    },
    {
      "word": "结为兄弟",
      "pinyin": "jié wéi xiōng dì",
      "meaning": "to become sworn brothers",
      "example": "他们在桃树下结为兄弟。"
    },
    {
      "word": "同心协力",
      "pinyin": "tóng xīn xié lì",
      "meaning": "to work together with one heart",
      "example": "他们约定同心协力。"
    },
    {
      "word": "乡亲",
      "pinyin": "xiāng qīn",
      "meaning": "neighbors from the same village",
      "example": "他们照看乡亲。"
    }
  ],
  "questions": [
    {
      "id": "q1",
      "type": "single_choice",
      "prompt": "刘备看到榜文后最想做什么？",
      "options": [
        "保护乡里",
        "去摘桃子",
        "独自旅行"
      ],
      "answer": "保护乡里",
      "explanation": "正文说刘备想着让百姓过安稳日子。"
    },
    {
      "id": "q2",
      "type": "single_choice",
      "prompt": "三个人在哪里结为兄弟？",
      "options": [
        "桃园",
        "江边",
        "城楼"
      ],
      "answer": "桃园",
      "explanation": "张飞家后有桃园，他们在桃树下结为兄弟。"
    },
    {
      "id": "q3",
      "type": "single_choice",
      "prompt": "这个故事最想告诉我们什么？",
      "options": [
        "朋友齐心能守护善良",
        "遇事只靠一个人",
        "不要听别人说话"
      ],
      "answer": "朋友齐心能守护善良",
      "explanation": "三人约定同心协力，互相提醒，照看乡亲。"
    }
  ],
  "retell_prompt": "说说刘备、关羽、张飞为什么愿意结为兄弟。"
}
""".trimIndent()
