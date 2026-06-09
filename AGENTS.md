# AGENTS.md — 小小中文经典 / Little Mandarin Classics

> 本文件是本仓库**权威**的 agent 指南，所有 agent（Claude、Codex 及其各会话）必须遵守。
> `CLAUDE.md` 是指向本文件的软链；`.agents/` 与本文件为准。完整设计见 `docs/specs/2026-06-10-reading-platform-design.md`。

## 1. 项目概述
面向**海外 5-8 岁华人/双语儿童**的"中文经典故事 AI 陪读"App。每天 5-8 分钟读一篇 300-600 字经典故事，含拼音、生字、朗读、3 道理解题、受控 AI 解释；家长可见进度。首批内容以**《三国演义》按原著顺序改写的儿童版**为主。

## 2. 工作区结构（monorepo）
```
apps/reader/{shared,androidApp,iosApp}   # ① KMP 移动端：shared 纯逻辑 / Android Compose / iOS SwiftUI
content/{schema,sources,stories}          # 内容数据(app 构建时打包)
pipeline/{scraper,transformer,validator}  # ② 内容抓取/创建(Python)
release/                                   # 上架产物/文案/合规
.agents/shared/<name>.md   # 权威·共享 agent 规范(唯一真相源)
.agents/skills/             # 安装/软链的 skills
.claude/agents/<name>.md   # Claude 子代理壳   .codex/agents/<name>.toml  # Codex 自定义 agent 壳
docs/specs/                                # 设计文档
```

## 3. 操作模式（本迭代 = MVP）
- **目标**：MVP，P0 功能**端到端跑通**；Android 为验证基准，iOS 待完整 Xcode。
- **自治执行，不停下来等人工确认。** 主 agent(Claude) 编排，Codex 执行，**agent 间自行沟通与交叉验证**，遇问题自动迭代修复直至通过。
- **交叉验证（每批产出必须过）**：
  - 代码：`test-author` 测试通过 + `code-reviewer` 评审 + 构建可跑（`./gradlew :androidApp:assembleDebug`）。
  - 内容：`content-safety-reviewer`（适龄/安全）+ `story-qa-validator`（schema/字数/答案一致）双闸。
- **完成定义(DoD)**：见设计 §11；Android 全部跑通即达本迭代 MVP。

## 4. Agent 体系（三处目录，格式不可互换）
- **共享规范（权威·单一真相源）**：`.agents/shared/<name>.md` —— 各专项 agent 的核心职责/输入/输出/约束/流程。Claude 与 Codex 都先读对应共享规范。
- **Claude 子代理壳**：`.claude/agents/<name>.md`（YAML frontmatter `name`/`description`；正文指向并遵循对应 `.agents/shared/<name>.md`）。Claude 递归扫描此目录、按 frontmatter `name` 注册。
- **Codex 自定义 agent 壳**：`.codex/agents/<name>.toml`（`name`/`description`/`developer_instructions`；指向并遵循对应共享规范）。
- **格式不可互换**：`.claude/agents/*.md` 与 `.codex/agents/*.toml` 是各自工具的原生格式；让 Codex *读取* `.agents/shared/*.md` 只是当作参考规范，**并不**等于把它注册成原生 subagent。新增/改名 agent 时三处同步（以共享规范为准，壳可由脚本再生）。
- **派发给 Codex 时**：鼓励 Codex 在任务内部 **spawn 自己的 Codex subagents** 并行处理相互独立的子任务并汇总结果。
- 注册表（22）见设计 §7.2；内容类经 `pipeline/transformer` 调用。

## 5. 工程约定（Engineering）
### KMP
- `shared` = **纯 Kotlin 业务逻辑，禁止任何平台类型**（无 `android.*`、无 `UIKit`、不用 JVM-only API）；只用 multiplatform + `kotlinx`。
- 序列化 `kotlinx.serialization`；异步 `kotlinx.coroutines` + `Flow`。
- 平台差异用 `expect/actual`（持久化、TTS）。
- 包名 / iOS bundle id：`com.littlemandarin.classics`。
### Android（Compose）
- Compose + Material3 + Navigation；`minSdk 24`、`targetSdk` 取最新稳定。
- 复用 `.agents/skills` 内 `jetpack-compose-m3`/`navigation-3`/`edge-to-edge`/`styles`/`testing-setup`。
### iOS（SwiftUI）
- SwiftUI 消费 `shared` 输出的 XCFramework；**当前仅搭骨架**，完整 Xcode 就绪后再构建/验证。
### i18n（强制，面向海外）
- 首期内建多语言：默认 **English + 简体中文(zh-Hans)**，架构可扩展。
- UI 文案一律走平台资源（Android `res/values*/strings.xml`、iOS String Catalog），**禁止硬编码 UI 文案**；`shared` 只返回 locale 无关数据/键。
- 故事正文保持中文（产品核心）；标题双语、生字英文释义。
### 测试与构建
- `shared` 用 `kotlin.test`（`commonTest`）；提交前测试必过。
- **构建验证**：codex 必须实际运行 gradle 任务并确保通过；失败先修复再标记完成。

## 6. 内容规则（Content）
- **数据契约**：`content/schema/story.schema.json`（详见 skill `story-json-format`）。每篇 300-600 字、Level 1-3、含拼音/5-8 生字/恰好 3 道单选题/1 复述提示、双语标题、生字英文释义。
- **来源**：仅**公有领域**底本（《三国演义》等），记录 `source_note`/`source_url`。首批清单见设计 §6.1。
- **儿童安全红线**：不出现血腥/恐怖/暴力细节/成人内容；战争桥段**淡化**为智慧、勇气、合作、仁义等正向主题；价值观正向、语言适龄。
- 产出落 `content/stories/<id>/story.json`(+封面)；经 `validator` 全过方可入库。

## 7. 隐私与合规红线（儿童 App）
- 遵守 **COPPA / GDPR-K**：**不收集儿童真实姓名/个人身份信息**；仅家长账户，数据最小化。
- AI 只做**受控解释**（围绕当前故事），**不做开放聊天**；离题温和拒绝："这个问题和今天的故事关系不大，我们先回到故事里吧。"
- 埋点匿名化，不含儿童 PII。

## 8. Skills
- 权威位置 `.agents/skills/`；Claude 侧经 `.claude/skills`（软链）发现。
- 相关时务必使用：Compose/Navigation/测试，以及项目 skill `story-json-format`、`kmp-shared-logic`。

## 9. 工具与命令
- JDK 21；Android SDK 在 `~/Library/Android/sdk`（`ANDROID_HOME` 已设）。
- Gradle 用 wrapper（`apps/reader/gradlew`）：`./gradlew :androidApp:assembleDebug`、`./gradlew :shared:allTests`。
- 内容流水线：Python（`pipeline/`）；转化经 `codex exec` 调 `.agents/agents` 内容 agent。
- iOS 构建：待完整 Xcode。

## 10. Git 约定
- 在**里程碑**（每批完成且验证通过）提交；信息格式 `<scope>: <做了什么>`（如 `shared: add Story model + repository`）。
- 主 agent(Claude) 的提交结尾加：`Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`。
- 不提交构建产物/密钥（见 `.gitignore`）。
