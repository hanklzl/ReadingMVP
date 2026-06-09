# 小小中文经典 / Little Mandarin Classics

面向**海外 5-8 岁华人/双语儿童**的"中文经典故事 AI 陪读"App。每天 5-8 分钟读一篇 300-600 字经典故事，含拼音、生字、朗读、3 道理解题、受控 AI 解释；家长可见进度。首批内容以**《三国演义》按序改写的儿童版**为主。

## 工作区（monorepo）
| 目录 | 说明 |
|---|---|
| `apps/reader/` | KMP 移动端：`shared`(纯业务逻辑) · `androidApp`(Compose) · `iosApp`(SwiftUI) |
| `content/` | 故事数据：`schema/`(契约) · `sources/`(公有领域底本) · `stories/`(成品) |
| `pipeline/` | 内容抓取/创建(Python)：`scraper` · `transformer` · `validator` |
| `release/` | 上架产物/文案/合规清单 |
| `.agents/` | **权威** AI agent 定义与 skills（`CLAUDE.md`/`​.claude` 为软链） |
| `docs/specs/` | 设计文档 |

## 开发
- 环境：JDK 21、Android SDK（`ANDROID_HOME`）、Python 3、Codex CLI。
- Android：`cd apps/reader && ./gradlew :androidApp:assembleDebug`
- 测试：`./gradlew :shared:allTests`
- iOS：待完整 Xcode 就绪。
- 内容流水线：见 `pipeline/`（转化经 `codex exec` 调 `.agents/agents` 内容 agent）。

## 给 agent
先读 **`AGENTS.md`**（权威通则）与 `docs/specs/2026-06-10-reading-platform-design.md`（总体设计）。

## 状态
MVP 开发中（Android 为验证基准；iOS 待 Xcode）。
