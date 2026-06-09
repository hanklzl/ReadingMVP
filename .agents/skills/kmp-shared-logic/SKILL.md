---
name: kmp-shared-logic
description: Use when working in the KMP shared module — enforces pure-Kotlin business logic with no platform types, kotlinx.serialization, coroutines/Flow, expect/actual for platform services, and kotlin.test coverage in commonTest
---

# KMP Shared Logic Conventions

`apps/reader/shared` 只放**跨平台业务逻辑**，UI 由各端实现（Android Compose / iOS SwiftUI）。

## 规则
- **禁止平台类型**：不得出现 `android.*`、`UIKit`、`java.*` 等平台限定 API；只用 Kotlin multiplatform + `kotlinx`。
- **模型**：`@Serializable`（`kotlinx.serialization`）；与 `content/schema/story.schema.json` 一一对应（`Story/Paragraph/Vocab/Question`）。
- **异步**：`kotlinx.coroutines` + `Flow`；仓库/用例返回挂起函数或 Flow。
- **平台差异**：用 `expect/actual`，在 `androidMain`/`iosMain` 各自实现（如本地存储、TTS）。
- **数据**：`StoryRepository` 读取打包进资源的故事 JSON。
- **DI**：轻量手写工厂/构造注入，不强依赖框架。

## 测试
- `commonTest` 用 `kotlin.test`，覆盖模型解析、仓库、测验评分、报告聚合。
- 提交前 `./gradlew :shared:allTests` 必须通过。

## 目录
```
shared/src/commonMain/kotlin   # 模型/仓库/服务接口/用例
shared/src/androidMain/kotlin  # actual 实现(Android)
shared/src/iosMain/kotlin      # actual 实现(iOS)
shared/src/commonTest/kotlin   # kotlin.test
```
