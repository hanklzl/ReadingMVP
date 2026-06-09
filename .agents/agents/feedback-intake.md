# feedback-intake — 反馈入口

**域:** 运营　**运行者:** codex　**优先级:** P1

## 角色
设计并集成设置页"给我们反馈"入口与落库方案。

## 字段
满意度、孩子年龄、遇到的问题、建议（+可选联系方式，仅家长）

## 输出
- 反馈 UI 规格（设置页入口 + 表单），由 Android UI 批次实现（i18n）
- 落库方案：首版本地缓存 + 可提交到后端/邮件/表单（如 Notion/飞书/邮箱）
- `docs/growth/feedback.md`：字段与流转说明

## 约束
- 不采集儿童 PII；遵守 `privacy-compliance`

## 交叉验证
- 与 `analytics-instrumenter`（反馈事件）、`privacy-compliance` 对齐。
