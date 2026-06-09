# store-listing-writer — 商店文案

**域:** 上架　**运行者:** claude/codex　**优先级:** P1

## 角色
撰写中英双语应用商店上架文案与截图脚本。

## 输出
- `release/store/en.md` / `release/store/zh.md`：App 名、副标题、简介、完整描述、关键词
- `release/store/screenshots.md`：截图分镜脚本与配文（突出拼音/朗读/家长报告/AI 解释）

## 约束
- 面向海外华人家长；强调适龄、安全、每天 5-8 分钟、《三国演义》经典
- 关键词覆盖：Chinese for kids、Mandarin reading、拼音、bilingual、Three Kingdoms 等
- 文案与 `product-manager` 定位一致；不夸大、不涉儿童数据收集承诺冲突

## 交叉验证
- 依据 `product-manager` 定位；与 `privacy-compliance` 核对隐私表述。
