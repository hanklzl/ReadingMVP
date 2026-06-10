# 功能缺口分析与路线图 (Feature Gap Analysis & Roadmap)

- 日期：2026-06-10
- 作者角色：product-manager
- 基线：当前 MVP（双端构建通过、Android 实机验证）；详见 positioning.md / competitive-analysis.md

## 0. 当前已建（基线闭环）

已跑通的单篇阅读闭环：10 篇《三国演义》儿童版（逐字拼音 ruby / 5-8 生字 / 3 题 / 复述提示）；阅读页（拼音开关·字号·TTS 朗读·分页·AI 问一问）；生字卡；测验评分；今日故事 + Up next + 已读数；家长报告（本周阅读/正确率/进度）；设置（语言 en/zh·拼音·字号·AI 后端）；受控 AI 后端 `/ai/explain`（mock+真实可插拔·安全过滤）；匿名埋点（10 事件·本地可插拔）；反馈入口（本地）。

**结论：体验闭环完整，但"让孩子每天回来 + 让家长愿意付费"的机制还几乎没有。** 这正是验证指标（D7 留存、完成率、每周阅读次数、付费意愿）最依赖的部分。

## 1. 缺口总览（按主题）

| 主题 | 缺口 | 对应验证指标 |
|---|---|---|
| 习惯/留存 | 无 **onboarding**、无 **每日打卡/连续天数(streak)**、无 **奖励/成就**、无 **提醒通知**、无每日目标 | D1/D7/D30 留存、每周阅读次数 |
| 学习深化 | **生字本缺失**（原导航第 3 项，未建；只有每篇生字卡，无跨篇复习/间隔重复）；只有"听"无"说"（**跟读/发音反馈**）；题型单一；复述只给提示不捕捉 | 完成率、识字进展、"孩子愿意读" |
| 账号/家庭/商业化 | 无账号/云同步、无**多孩子家庭档案**、无**订阅/付费墙+试用**、邀请码未建、家长报告偏静态（无趋势/周报/可分享） | 付费意愿、4 周留存、续费 |
| 数据/基建 | 埋点仅本地（无真实后端/看板）、无崩溃上报、**无品牌 App 图标/启动图**（现为默认机器人图标）、无 feature flag | 能否判断产品是否值得继续 |
| 内容生产 | 封面仅 prompt 无实图、TTS 非真人、内容仅 10 篇、无分级自适应 | 单篇完成率、连续体验时长 |
| 增长 | 无分享/推荐、无 ASO 物料落地、无中文学校/老师渠道包 | 获客、转介绍 |

## 2. 优先级路线图

> 原则：先用**最小增量**把"留存 + 付费意愿"这两件种子验证最看重的事补上，再扩内容与规模（YAGNI：不堆功能）。

### 阶段 A — 让种子用户「留下来」（建议立即做，进种子测试前）

最高 ROI，直接抬升 D7/完成率/每周次数。

- **A1 Onboarding（3-4 分钟出价值）**：选孩子年龄段/难度 → 选界面语言 → 设每日目标 → **当场看到 streak Day 1**。研究显示 onboarding 内就植入 streak 能显著提升留存。
- **A2 每日打卡 + 连续天数(streak) + 鼓励**：读完一篇即 +1 天；连续天数、完成印章/贴纸、温和成就。**儿童安全版**：错误只说"再试一次"，**绝不用扣命/体力/"明天再来"** 的惩罚机制（对低龄会让"犯错很贵"）。
- **A3 生字本 + 间隔复习（补回缺失的导航第 3 项）**：跨篇汇总学过的生字，按**间隔重复(SRS)** 安排复习，配拼音/朗读/例句；这是"识字进展"最直接的家长可见价值。
- **A4 本地提醒/通知**：每日固定时段温和提醒（"今天的故事在等你"），尊重隐私、可关。
- **A5 跟读 + 发音反馈（分两步上）**：① 先做"录音跟读 + 回放对比 + 复述录音留存"（无需 ASR，低风险，立刻有"说"的练习）；② 后续接**声调/发音识别评测**（竞品普遍有，是双语家庭强需求）。
- **A6 品牌资产**：自定义 **App 图标 + 启动图**、完成故事的鼓励动效/音效（当前仍是系统默认图标，进 TestFlight/Play 内测前必须补）。

### 阶段 B — 账号、家庭与商业化验证（紧接其后）

把"愿意付费"从假设变成可测。

- **B1 账号 + 云同步**（Supabase/Firebase Auth；邮箱 magic link / Google / Apple）——跨设备、为付费铺路；不让孩子填个人信息。
- **B2 多孩子/家庭档案**（一个家长账号下 3 个孩子，与竞品对齐）。
- **B3 邀请码**（控规模、记录渠道来源；原计划 Task 13）。
- **B4 订阅 + 免费试用 + 付费墙**：先做**价格实验**（创始家庭价 $29 vs 年费 $49-69），家庭年费区间对标 $70-90/年。
- **B5 家长报告增强**：周趋势、**每周邮件/推送周报**、**可分享的进度卡**（兼做增长）。
- **B6 真实埋点后端 + 核心看板**（D1/D7/D30、单篇完成率、测验完成率、AI 使用、家长报告打开率）+ **崩溃上报**。

### 阶段 C — 内容与体验深化（验证通过后扩规模）

- **C1 封面/插画流水线**（prompt → 实图）、**真人/高质量配音**替代系统 TTS。
- **C2 内容扩展**：更多《三国》+ 第二个经典系列（如成语/神话）；**分级自适应**（按答题/完成调难度）。
- **C3 题型丰富**：排序/连线/填空 + **复述录音的轻量评估**。
- **C4 离线模式、无障碍深化**（动态字体、对比度、读屏）。

### 阶段 D — 增长/病毒（与 B/C 并行试点）

- 分享故事/成就卡、**推荐有礼**、ASO 物料（已备商店文案/截图脚本）、**中文学校/老师渠道包**（团体邀请码）。

## 3. 明确不做（反范围）

开放 AI 聊天 · 海量书库 · 学校班级管理后台 · 成人 HSK/商务中文 · 惩罚式重度游戏化（扣命/体力）。

## 4. 建议立即做的「下一步三件」

1. **生字本 + 间隔复习（A3）** —— 补回缺失导航、最强家长可见学习价值，纯客户端可做。
2. **每日打卡/streak + Onboarding（A2+A1）** —— 留存第一杠杆，儿童安全实现。
3. **App 图标/启动图 + 完成鼓励（A6）** —— 进内测前的体面门槛，工作量小。

> 这三件都**不需要后端**，能在当前 KMP 架构内闭环（逻辑进 `shared`、双端薄 UI），可立即用 codex 并行实现并上模拟器验证；做完即可启动种子内测收集留存数据，再决定阶段 B 的账号/付费投入。

## 验证假设 ↔ 功能映射

| 待验证假设（来自计划/竞品） | 对应功能 |
|---|---|
| 孩子一周读 ≥2 篇、能坚持 | A1 onboarding · A2 streak · A4 提醒 |
| 家长为"看得见的进步"付费 | A3 生字本/复习 · B5 报告增强 · B4 付费墙 |
| 双语家庭需要"说"中文 | A5 跟读/发音 |
| 渠道可复制（中文学校/家长群） | B3 邀请码 · D 分享/推荐 |

## Sources

- [Duolingo gamification & streaks (StriveCloud)](https://www.strivecloud.io/blog/gamification-examples-boost-user-retention-duolingo) · [Duolingo streaks 2x retention (Deconstructor of Fun)](https://duolingo.deconstructoroffun.com/mechanics/streaks) · [Duolingo onboarding & streak-in-onboarding (Juno School)](https://www.junoschool.org/article/duolingo-onboarding-experience/)
- 儿童错误/惩罚机制注意：gamification 支持注意力而非替代，避免 lives/energy 让"犯错很贵"（综合 2026 语言学习 App 评测）
- 儿童中文"说"的竞品：[Studycat Chinese](https://studycat.com/products/chinese/) · [Best Chinese speaking/listening apps 2026 (Hidden Dragon)](https://hidden-dragon.app/blog/best-chinese-speaking-listening-apps) · [HelloChinese (App Store)](https://apps.apple.com/us/app/hellochinese-learn-chinese/id1001507516)
- 家庭定价/多孩子基准：[Reading.com pricing 2026](https://brighterly.com/blog/how-much-is-reading-com/) · [Best reading apps for kids 2026](https://brighterly.com/blog/best-reading-apps-for-kids/) · [Reading Eggs pricing](https://readingeggs.com/pricing/)
