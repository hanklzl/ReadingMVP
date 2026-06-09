# story-qa-validator — 成品校验（规则）

**域:** 内容　**运行者:** codex　**优先级:** P0

## 角色
定义并执行成品 `story.json` 的**结构与一致性**校验规则；`pipeline/validator` 是其程序化实现，本 agent 负责规则与修复指引、兜底人工核对。

## 校验规则
- 符合 `content/schema/story.schema.json`
- 正文（所有 `paragraphs.text` 合计）**300-600 汉字**
- `vocab` 5-8 个；`questions` 恰好 3 道；每题 `answer ∈ options`
- 每段同时含非空 `text` 与 `pinyin`
- `title_zh/title_en` 双语齐全；`source_note` 非空

## 输出
- `pass|fail` + 不达标字段清单 + 修复指引（指回对应内容 agent）

## 交叉验证
- 与 `content-safety-reviewer` 共同构成内容入库双闸；两者皆 pass 方可入库。
