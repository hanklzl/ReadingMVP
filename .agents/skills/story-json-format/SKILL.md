---
name: story-json-format
description: Use when authoring, transforming, or validating story content for Little Mandarin Classics — defines the required fields, constraints, bilingual + child-safety rules for content/stories/<id>/story.json against content/schema/story.schema.json
---

# Story JSON Format

每个故事产出一个 `content/stories/<id>/story.json`，必须符合 `content/schema/story.schema.json`。

## 字段
- `id` kebab-case 唯一标识（如 `peach-garden-oath`）
- `title_zh` / `title_en` 中英标题（双语）
- `level` 1-3 难度；`age_range` 如 `5-6` / `7-8`
- `source_note` 出处说明；`source_url` 公有领域底本链接（可选）；`cover_image` 封面相对路径（可选）
- `paragraphs[]`：每段 `{text, pinyin, cells}`；`pinyin` 是整段拼音，多音字按语境；`cells` 是与 `text` Unicode 字符逐字对齐的数组
- `paragraphs[].cells[]`：每项只能是 `{ "c": "单个字符", "p": "该字拼音或空" }`；汉字 `p` 必须为 pypinyin `Style.TONE` 带调拼音，标点/空格/拉丁/数字等非汉字 `p` 必须为 `""`
- `vocab[]`：**5-8 个**，每个 `{word, pinyin, meaning(英文), example?}`
- `questions[]`：**恰好 3 道** `single_choice`，每题 `{id, type, prompt, options(2-4), answer, explanation}`
- `retell_prompt`：一句复述提示

## 硬约束（validator 会校验）
- 正文（所有 `paragraphs.text` 合计）**300-600 个汉字**
- `vocab` 5-8 个；`questions` 恰好 3 道；每题 `answer ∈ options`
- 每段必须同时给 `text`、`pinyin` 与 `cells`
- `cells.length` 必须等于 `text` 的 Unicode 字符数；每个 `cells[i].c` 必须等于 `text[i]`
- 汉字范围：CJK Unified、Extension A、Compatibility；这些字符的 `p` 非空，其余字符的 `p` 为空字符串

## 来源与儿童安全
- 仅**公有领域**底本（首批《三国演义》），记录出处
- 适龄、**无血腥/恐怖/暴力细节/成人内容**；战争桥段淡化为智慧/勇气/合作/仁义等正向主题
- 价值观正向、语言适合 5-8 岁

## 最小示例
```json
{
  "id": "peach-garden-oath",
  "title_zh": "桃园三结义",
  "title_en": "The Oath of the Peach Garden",
  "level": 1,
  "age_range": "5-8",
  "source_note": "Based on public-domain 《三国演义》, rewritten for children.",
  "paragraphs": [
    {
      "text": "桃园，A1",
      "pinyin": "táo yuán，A1",
      "cells": [
        { "c": "桃", "p": "táo" },
        { "c": "园", "p": "yuán" },
        { "c": "，", "p": "" },
        { "c": "A", "p": "" },
        { "c": "1", "p": "" }
      ]
    }
  ],
  "vocab": [{ "word": "结义", "pinyin": "jié yì", "meaning": "to become sworn brothers", "example": "他们在桃园结义。" }],
  "questions": [{ "id": "q1", "type": "single_choice", "prompt": "三个人在哪里结为兄弟？", "options": ["桃园", "山上", "河边"], "answer": "桃园", "explanation": "故事里他们在开满桃花的园子里结义。" }],
  "retell_prompt": "用一句话说说：三个人为什么要结为兄弟？"
}
```
