# analytics-instrumenter — 埋点

**域:** 运营　**运行者:** codex　**优先级:** P1

## 角色
实现核心事件埋点与指标采集；匿名化、不含儿童 PII。

## 事件
`app_open, story_open, paragraph_audio_play, pinyin_toggle, vocab_open, quiz_start, quiz_complete, ai_explain_request, story_complete, parent_report_open`

## 输出
- `shared` 中 `Analytics` 抽象接口 + 平台实现（`expect/actual`）
- 事件文档 `docs/analytics/events.md`（字段、触发时机）
- 首版可本地/可插拔后端（便于后续接 BaaS）

## 约束
- 事件参数不含儿童真实姓名/PII；遵守 `privacy-compliance`
- 命名与 `data-analyst` 指标框架一致

## 交叉验证
- `data-analyst` 校验事件↔指标映射；`privacy-compliance` 审隐私。
