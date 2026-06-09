# 海外华人儿童中文经典阅读平台 — 总体设计 (Spec)

- **日期:** 2026-06-10
- **状态:** 待评审 (Draft, awaiting approval)
- **来源:** `2026-06-09_230433-overseas-chinese-reading-app-mvp.md`（产品计划）
- **主 agent (编排):** Claude Code　|　**执行 sub-agent:** Codex (via MCP `codex` / `codex-reply`)

---

## 1. 背景与目标

为海外 5-8 岁华人/双语儿童做一个"中文经典故事 AI 陪读"移动端 MVP：每天 5-8 分钟读一篇 300-600 字的经典故事，配拼音、生字、朗读、3 道理解题、受控 AI 解释；家长可见进度。目标是拿到第一批种子用户验证需求。

本 spec 在产品计划基础上，按用户决策扩展为一个 **monorepo 平台**，含：① 移动端 App、② 内容抓取/创建流水线、③ 一支可复用的 AI agent 体系。

## 2. 决策与范围

技术与范围已拍板：

| 维度 | 决策 |
|---|---|
| 移动端 | **KMP 标准工程**：`shared` 只共享业务逻辑（纯 Kotlin、无 UI）；Android = **Compose**；iOS = **SwiftUI** |
| 内容来源 | **抓取公有领域底本 + AI 改写**（出处可溯源） |
| 内容流水线技术栈 | **Python** |
| 转化 agent 执行 | **codex exec**（流水线调用 codex 运行 `.agents` 内的 agent，无需 API key） |
| iOS | **现在搭骨架**；完整 Xcode 安装中，iOS 构建/验证待其就绪；当前在 **Android 上先验证** |
| 首批内容 | **《三国演义》为主**，按原著顺序改写 **~10 篇**儿童版（公有领域·罗贯中）；提议清单见 §6.1，最终由内容 agent 策展/适龄化 |
| 多语言 i18n | **首期即内建**：UI 默认 **English + 简体中文**、架构预留更多语言；故事正文中文为主、标题/生字释义双语 |
| Agent 范围 | **全部 5 组**（内容 / 上架+合规 / 工程 / 运营+增长 / 产品+数据） |
| AI 文件约定 | `AGENTS.md` 与 `.agents/` 为**权威**；Claude 侧用**软链** |

**P0/P1/P2**（沿用产品计划 §6）：
- **P0**：故事列表、阅读页、拼音开关、生字词、朗读、阅读题、完成记录 + 10 篇内容
- **P1**：家长报告、反馈入口、受控 AI 解释、邀请码、基础埋点
- **P2（暂不做）**：支付订阅、老师端、班级、复杂游戏化、真人配音、大型内容后台、完整多语言

**首版不做**（沿用 §0）：海量书库、自由 AI 聊天、社区、复杂老师端、正式支付、复杂游戏化。

## 2.1 操作模式（本迭代）

- **目标产出：** MVP 版本，所有 P0 基础功能**端到端跑通**（P1 尽量纳入）。
- **自治执行：** 过程**不需人工确认**。主 agent(Claude) 负责编排，codex 执行，**agent 间自行沟通与交叉验证**，遇问题自动迭代修复，直至通过。
- **交叉验证协议：** 代码 → `test-author` 跑测试 + `code-reviewer` 评审 + 构建必须可跑通；内容 → `content-safety-reviewer` + `story-qa-validator` 双闸；任一不过即自动返工。
- **完成定义(DoD)：** 对齐 §11 验收清单；**Android 端全部跑通**即视为本迭代 MVP 达成；iOS 待 Xcode 就绪后验证。

## 3. 总体架构（monorepo）

```
ReadingMVP/
├─ AGENTS.md                     # 权威·工程通则(所有 codex 会话遵守)
├─ CLAUDE.md  → AGENTS.md         # 软链
├─ .agents/                      # 权威
│  ├─ agents/                    #   可复用专项 agent 定义(每个一个 .md)
│  └─ skills/                    #   安装/软链的 skills
├─ .claude/
│  └─ skills → ../.agents/skills # 软链(Claude 侧发现 skills)
├─ docs/
│  └─ specs/                     # 本 spec 及后续设计文档
├─ apps/
│  └─ reader/                    # ① KMP 移动端(独立 Gradle 构建)
│     ├─ shared/                 #     纯业务逻辑(Kotlin, 无 UI)
│     ├─ androidApp/             #     Compose UI
│     └─ iosApp/                 #     SwiftUI UI(消费 shared framework)
├─ content/                      # 内容数据(app 构建时打包)
│  ├─ schema/story.schema.json
│  ├─ sources/                   #   抓取的公有领域底本 + 出处/许可记录
│  └─ stories/<id>/              #   成品 story JSON + 封面
├─ pipeline/                     # ② 内容抓取/创建工程(Python)
│  ├─ scraper/  transformer/  validator/
│  └─ pyproject.toml
└─ release/                      # 上架产物：商店文案/截图脚本/合规清单
```

三大块职责：**移动端** ← 读 `content/stories`；**流水线** → 产 `content/stories`；**agent 体系** = 流水线与开发/上架/运营的"工人"。

## 4. 移动端设计（KMP）

- **`shared`（纯业务逻辑，无 UI）**
  - 模型：`Story / Paragraph / Vocab / Question`（`kotlinx.serialization`）
  - 仓库：`StoryRepository`（首版读打包进资源的本地 JSON）
  - 服务：`ProgressService`（本地存储进度）、`TtsService`（`expect/actual` 接平台 TTS）、`AiService`（受控问答客户端，P1）
  - 用例：故事列表、阅读、测验评分、进度统计、家长报告聚合
  - 平台差异用 `expect/actual`（存储、TTS）；轻量手写 DI（不强依赖框架）
  - 测试：`kotlin.test` 覆盖模型解析、仓库、评分、报告聚合
- **`androidApp`（Compose）**：Material3 + Navigation；页面=故事库/阅读/生字/测验/家长报告/设置；UI 依据 `ui-designer` 产出的设计系统/mockup 实现；复用已装 skills（`jetpack-compose-m3`、`navigation-3`、`edge-to-edge`、`styles`、`testing-setup`）
- **`iosApp`（SwiftUI）**：消费 `shared` 输出的 XCFramework；页面与 Android 对齐；**待 Xcode 就绪后构建/验证**
- **多语言(i18n)**：首期内建；UI 文案走平台资源（Android `values*/strings.xml`、iOS String Catalog），`shared` 只暴露 locale 无关数据；默认 en + zh-Hans，架构可扩展；**禁止硬编码 UI 文案**
- **边界守护**：`shared` 不得引入任何 Android/iOS 平台类型（由 `kmp-architect` + AGENTS.md 约束）
- **数据流**：`stories.json`(资源) → `StoryRepository` → UseCase → 平台 ViewModel/ObservableObject → UI

## 5. 内容流水线设计（`pipeline/`, Python）

抓取 → 改写 → 标注 → 出题 → 安全审查 → 校验 → 成品，端到端：

1. **`scraper`**：按故事清单抓取公有领域底本（维基文库 / 古诗文网 / Project Gutenberg 等），写入 `content/sources/<id>/`，并记录 `source_url` + 许可状态。
2. **`transformer`**：编排 `.agents/agents/` 里的内容 agent，**通过 `codex exec` 逐篇运行**，产出 `content/stories/<id>/story.json` + 封面绘图 prompt。
3. **`validator`**：校验产出符合 `story.schema.json`、字数 300-600、拼音段落对齐、题目 `answer ∈ options`、生字 5-8 个。

构建步骤把 `content/stories` 同步进 app 资源（Android assets / iOS bundle）。

## 6. 内容数据格式

`content/schema/story.schema.json`（JSON Schema, draft 2020-12），字段对齐产品计划 §2：`id, title_zh, title_en, level(1-3), age_range, source_note, source_url, cover_image, paragraphs[{text,pinyin}], vocab[{word,pinyin,meaning,example}], questions[{id,type,prompt,options,answer,explanation}], retell_prompt`。`shared` 的 Kotlin 模型与本 schema 一一对应，由 `story-json-format` skill 固化约定。

### 6.1 首批内容：《三国演义》按序改写（提议）

公有领域底本（罗贯中《三国演义》）。按原著时间线选取适龄、低暴力的桥段，战争场面由 `content-safety-reviewer` 淡化；最终清单/顺序由 `source-scout`+`story-rewriter`+`content-safety-reviewer` 策展确定。提议 10 篇：

1. 桃园三结义 `peach-garden-oath`（义气·友情）
2. 三英战吕布 `three-heroes-vs-lubu`（勇敢·团队；战斗淡化）
3. 望梅止渴 `quench-thirst-plums`（智慧·成语）
4. 三顾茅庐 `three-visits-cottage`（坚持·尊重）
5. 赵云长坂坡救主 `zhaoyun-changban`（忠勇；战斗淡化）
6. 草船借箭 `borrow-arrows-boats`（智慧）
7. 火烧赤壁 `red-cliffs`（计谋·合作；战火淡化）
8. 关羽华容道义释曹操 `huarong-path`（仁义·知恩图报）
9. 七擒孟获 `seven-captures`（以德服人·宽容）
10. 空城计 `empty-fort`（沉着·机智）

每篇 300-600 字、Level 1-3 分级，含拼音/5-8 生字/3 题/复述提示，结构遵 `story.schema.json`。

## 7. AI Agent 体系

### 7.1 文件与软链约定（`.agents` / `AGENTS.md` 为准）

- `AGENTS.md`（**权威**）：工程通则 —— KMP 模块边界、Compose/SwiftUI 约定、`kotlinx.serialization`、测试与评审要求、内容 schema 位置、**儿童内容安全与隐私红线**、如何调用 `.agents/agents` 与 skills。
- `CLAUDE.md` → `AGENTS.md`（软链）。
- **Agent 体系（三处目录，格式不可互换）**：核心规范在 `.agents/shared/<name>.md`（**权威·单一真相源**）；`.claude/agents/<name>.md` 为 Claude 子代理壳(frontmatter)、`.codex/agents/<name>.toml` 为 Codex 自定义 agent 壳，二者均指向对应共享规范。让 Codex 读 `.agents/shared/*.md` 仅作参考，不等于注册成原生 subagent。派发 Codex 时鼓励其内部 spawn Codex subagents 并行。
- `.agents/skills/`（**权威**）：软链相关已装 Android skills（`jetpack-compose-m3`/`navigation-3`/`edge-to-edge`/`styles`/`testing-setup`/`android-cli`/`adaptive`）+ 新写项目 skill `story-json-format`、`kmp-shared-logic`。
- `.claude/skills` → `../.agents/skills`（软链）。

### 7.2 Agent 注册表（22 个）

| # | Agent | 域 | 职责 | 运行者 | 优先级 |
|---|---|---|---|---|---|
| 1 | `source-scout` | 内容 | 按清单定位公有领域底本 URL + 版权状态 | codex | P0 |
| 2 | `story-rewriter` | 内容 | 底本→**按指定年龄段(5-6/7-8→Level1/2/3)** 改写成 300-600 字儿童版 | codex | P0 |
| 3 | `pinyin-annotator` | 内容 | 逐段拼音（多音字按语境） | codex | P0 |
| 4 | `vocab-extractor` | 内容 | 选 5-8 生字 + 拼音/英文释义/例句 | codex | P0 |
| 5 | `quiz-author` | 内容 | 3 道单选题 + 1 复述提示 | codex | P0 |
| 6 | `content-safety-reviewer` | 内容 | 适龄/暴力恐怖/价值观/忠实度审查 | codex | P0 |
| 7 | `story-qa-validator` | 内容 | 校验 schema/字数/拼音/答案一致 | codex | P0 |
| 8 | `cover-art-prompter` | 内容 | 统一风格的封面绘图 prompt | codex | P0 |
| 9 | `release-manager` | 上架 | 版本/changelog/构建产物/内测渠道(Play Internal、TestFlight)步骤 | codex+人工 | P1 |
| 10 | `store-listing-writer` | 上架 | 中英商店文案/关键词/截图脚本 | claude/codex | P1 |
| 11 | `privacy-compliance` | 上架 | **儿童隐私(COPPA/GDPR-K)**、隐私政策、Data Safety/隐私标签、年龄分级 | claude/codex | P1 |
| 12 | `signing-setup` | 上架 | Android keystore / iOS 证书描述文件指引 | codex+人工 | P1 |
| 13 | `kmp-architect` | 工程 | 守 `shared` 无 UI 泄漏、expect/actual、依赖约定 | codex | P0 |
| 14 | `test-author` | 工程 | shared 单测 + 平台测试(遵 `testing-setup`) | codex | P0 |
| 15 | `code-reviewer` | 工程 | 代码评审(复用 superpowers code-review) | codex | P1 |
| 16 | `analytics-instrumenter` | 运营 | 实现埋点事件与指标(§Task15) | codex | P1 |
| 17 | `ai-explain-backend` | 运营 | 受控 AI 问答后端 `/ai/explain` + 安全过滤(Task10) | codex | P1 |
| 18 | `seed-user-recruiter` | 运营 | 筛选表单/招募话术/渠道清单(§4) | claude/codex | P1 |
| 19 | `feedback-intake` | 运营 | 设置页反馈入口 + 落库(Task14) | codex | P1 |
| 20 | `product-manager` | 产品 | App 定位、产品调研、市场空间(TAM/SAM/SOM)、竞品分析 | **claude+联网** | P1 |
| 21 | `data-analyst` | 数据 | 上架后分析埋点/指标→**开发指引**；预上架先定分析框架 | claude/codex | P1 |
| 22 | `ui-designer` | 设计 | 设计系统(色彩/字体/间距/圆角)、儿童友好视觉、各页 wireframe/mockup、组件与可访问性规范，交付 design tokens 给 Compose/SwiftUI | claude(+出图) | P0 |

> 工程类(13-15)亦在 AGENTS.md 中固化为通则；单列 agent 便于按需重复调用。设计(22)先于 UI 实现产出设计系统与各页 mockup。

## 8. 环境与构建

- ✅ Java 21 / Android SDK(build-tools/ndk/emulator) / codex 0.137 / Python 3.14 / Node 26
- ⚠️ 本机原为 Command Line Tools，**完整 Xcode 安装中** → iOS 构建/验证待其就绪
- Gradle 用 wrapper 生成；KMP 经 Gradle 构建；iOS framework 经 KMP 输出 XCFramework

## 9. Codex 派发批次（执行顺序）

- **批 0（主 agent 直接做）**：工作区骨架 + `AGENTS.md`/`CLAUDE.md` 软链 + `.agents/{agents,skills}`（含 21 个 agent 定义、skills 软链）+ `story.schema.json`。
- **批 1（codex）**：KMP 脚手架(`shared`+`androidApp`+`iosApp`)，Android 跑通首页"小小中文经典"。〔Task1〕
- **批 2（codex）**：`shared` 模型/仓库/用例 + 单测，本地 JSON 驱动。〔Task2〕
- **批 3（codex）**：Python 流水线(scraper/transformer/validator) + 用内容 agent 产出 **10 篇**。〔Task2 内容/Task12〕
- **批 4（codex）**：Android UI —— 故事库/阅读(拼音·字号)/生字/测验/朗读/进度/家长报告（先由 `ui-designer` 出设计系统 + 各页 mockup，再实现）。〔Task3-9〕
- **批 5（codex/主 agent）**：受控 AI 后端 + 埋点 + 反馈 + 邀请码；上架/合规产出物；`product-manager` 与 `data-analyst` 框架。〔Task10-15〕
- **iOS**：Android 验证稳定 + Xcode 就绪后，补 SwiftUI UI 并验证。

每批以 codex `cwd=对应目录`、`sandbox=workspace-write`、`approval-policy=never` 运行；主 agent 审查产出、回填问题、再进入下一批。

## 10. 风险与依赖

| 风险 | 应对 |
|---|---|
| iOS 暂不能本机验证 | 仅搭骨架；Xcode 就绪后单独验证批次 |
| 改写忠实度/版权 | 只用公有领域底本 + 记录出处 + `content-safety-reviewer` 把关 |
| 儿童隐私合规 | `privacy-compliance` 全程；不收集儿童真实姓名/个人信息 |
| AI 产出质量不稳 | safety + qa-validator 双闸 + 人工抽检；改写后端可切 codex/API |
| codex 批量产 10 篇耗时 | 分批跑、可断点续；先 2 篇验证模板再放量(可选) |

## 11. 验收（对齐产品计划 §10）

家长能装测试版；孩子能打开故事库、读完整篇、开拼音、听朗读、看生字、做完 3 题；家长能看已读记录；能提交反馈；团队能看基础使用数据。Android 全部跑通即达 MVP 门槛；iOS 待 Xcode 验证。
