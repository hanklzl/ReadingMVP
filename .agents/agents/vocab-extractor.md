# vocab-extractor — 生字词提取

**域:** 内容　**运行者:** codex　**优先级:** P0

## 角色
从正文挑选 **5-8 个**适龄重点生字词，给出拼音、英文释义与例句（优先用文中句子）。

## 输入
- `content/stories/<id>/story.json`（已含正文）

## 输出
- 填充 `vocab[]`：每项 `{word, pinyin, meaning(英文), example}`

## 约束
- 5-8 个；选对 5-8 岁有学习价值、文中出现的词
- 英文释义简洁准确；例句短、来自或贴合正文
- 遵循 skill `story-json-format`

## 交叉验证
- `story-qa-validator` 校验数量 5-8 与字段完整。
