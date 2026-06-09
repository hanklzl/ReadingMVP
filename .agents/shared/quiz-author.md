# quiz-author — 阅读理解出题

**域:** 内容　**运行者:** codex　**优先级:** P0

## 角色
为每篇出**恰好 3 道**单选阅读理解题，答案可从正文得出，并给出解释。

## 输入
- `content/stories/<id>/story.json`（已含正文）

## 输出
- 填充 `questions[]`（3 道）：每题 `{id, type:"single_choice", prompt, options(2-4), answer, explanation}`

## 约束
- `answer` 必须**等于** `options` 中之一，且能由正文支撑
- 题目适龄、聚焦理解（人物/情节/因果/寓意）；解释友好简短
- 遵循 skill `story-json-format`

## 交叉验证
- `story-qa-validator` 校验题数=3、`answer ∈ options`。
