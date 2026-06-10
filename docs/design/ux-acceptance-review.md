# UX 验收评审：全页面

日期：2026-06-10  
角色：product-manager Codex custom agent  
范围：今日故事、故事库、阅读页新版、生字本/生字学习、测验、家长报告、设置、Onboarding。  
资料来源：`AGENTS.md`、`.codex/agents/product-manager.toml`、`.agents/shared/product-manager.md`、Android `MainActivity.kt`、iOS `ContentView.swift`、`docs/design/`、`docs/product/`、`docs/qa/screenshots/`。

## 评审口径

- 阻断：影响 MVP 儿童安全/隐私、主路径闭环、设计验收可信度，或会导致儿童明显误操作。
- 重要：不阻断端到端，但会显著影响 5-8 岁儿童理解、家长信任、双端一致性或可访问性。
- 次要：体验打磨、文案层级、视觉一致性、后续可优化项。

本轮不改代码，只产出推动修复的产品与 UX 问题清单。Android 是当前验证基准；iOS 用于双端一致性检查。

## 总体结论

当前实现已经覆盖 MVP 主要页面和流程，但验收层面存在 5 类高优先级问题：

1. 家长区与设置区的成人边界不足：家长报告没有真正 gate，设置页暴露 AI backend 配置，反馈表有联系方式输入。
2. 今日故事的“每日主行动”不够稳定：完成态仍可能显示 Start reading，今日选择策略有固定第一篇的风险。
3. 新版阅读页需要收紧交互语义：句子正文点击、AI Ask 入口、设置 sheet 和底部栏需要更符合“故事优先、受控解释”。
4. 生字本已经成为顶级导航，但设计系统和 mockup 仍停留在旧 IA，需要补齐权威基线。
5. 截图验收证据不可靠：新版阅读页、故事库、生字本、测验缺有效截图，多张现有截图命名与实际页面不符。

## 逐页问题清单

### Onboarding

| 页面 | 严重度 | 优先级 | 问题 | 为何不合理 | 具体改进方案 |
|---|---|---:|---|---|---|
| Onboarding | 重要 | P1 | `Skip` 可直接跳过年龄段、语言、每日目标设置。 | 年龄段和每日目标会影响推荐、streak 和家长预期；海外儿童首次体验需要稳定默认值和隐私说明。 | 保留 Skip 时必须落安全默认值，并在 Today 或 Settings 提供“稍后设置阅读等级/每日目标”的补入口；若种子测试强调留存，首轮可移除 Skip。 |
| Onboarding | 重要 | P1 | 未完成故事前展示类似 “Streak Day 1” 的奖励语义。 | 儿童奖励必须准确，未完成阅读就显示连续天数会让 streak 失真。 | 改为 “Start Day 1 / 完成今日故事开始连续阅读”；只有测验完成后才显示 streak 增长。 |
| Onboarding | 重要 | P1 | 年龄选项容易被理解为儿童档案信息。 | 项目红线是不收集儿童真实身份和精确资料；年龄段可以用于难度，但需要表述为阅读等级偏好。 | 文案改为 “Reading level / 阅读等级”，并说明只用于本地推荐，不需要姓名、生日、学校等信息。 |
| Onboarding | 次要 | P2 | 视觉结构接近成人设置表单，儿童启动仪式感不足。 | 首次体验应该让孩子理解“每天读一个故事”，不是先进入配置中心。 | 改成三张大卡：阅读等级、显示语言、每日目标；选中状态用背景/边框/图标共同表达，不只靠颜色或 check。 |
| Onboarding | 次要 | P2 | iOS 初始 locale 先默认 English，再在 `onAppear` 同步。 | 首屏可能短暂显示错误语言，影响双语家庭的信任感。 | 在进入 Onboarding 前完成 settings load，或让 Onboarding state 初始化直接使用传入 locale。 |

### 今日故事

| 页面 | 严重度 | 优先级 | 问题 | 为何不合理 | 具体改进方案 |
|---|---|---:|---|---|---|
| 今日故事 | 阻断 | P0 | 今日故事选择存在固定第一篇的风险，完成态仍可能显示 “Start reading”。 | 产品定位是每日 5-8 分钟推进一个经典故事；如果一直锚定第一篇或完成后还提示开始阅读，主路径会失真。截图 `31-reading-redesigned.png` 也显示完成态仍有 Start reading。 | 明确 Today 策略：优先下一篇未完成故事；当前故事已完成时主 CTA 改为 “Read again”，并把 “Up next” 作为下一篇入口。 |
| 今日故事 | 阻断 | P0 | 顶层页面存在底部导航遮挡内容的截图证据。 | `22-today.png`、`31-reading-redesigned.png` 中 Up next 区域被 bottom nav 裁切，影响可读性和触控。 | 所有带 bottom nav 的页面统一增加 bottom nav + safe area 的内容 padding，最后一张卡片底部至少保留一个 `space.6`。 |
| 今日故事 | 重要 | P1 | Quiz tile 未完成阅读时仍像同级入口，只通过 snackbar/alert 解释锁定。 | 儿童会先尝试测验，打断“先读再测”的闭环；双端反馈形态也不一致。 | 锁定态视觉降级，显示小锁和 “Read the story first”；双端统一用非阻断轻提示。 |
| 今日故事 | 重要 | P1 | Summary tile 重复数字，例如 value 是 6，label 又是 “6 new words”。 | 信息扫描困难，孩子可能以为是两个不同指标。 | value 保留数字，label 改为 “New words / 生字” 和 “Quiz / 小测验”；说明文本放次级行。 |
| 今日故事 | 重要 | P1 | Completed 徽章在窄胶囊内断成 “Compl / eted”。 | 违反设计系统关于文本不应在容器内不自然断行的要求，且降低完成反馈的可信度。 | 改为短标签 “Done / 完成”，或增加 chip 宽度并禁止词内断行。 |
| 今日故事 | 次要 | P2 | 顶部 title 是 App 名，subtitle 才是 Today。 | 首屏主任务应该是“今日故事”，App 名不应抢占页面标题。 | title 改为 “Today story / 今日故事”，App 名放启动页或小副标题。 |
| 今日故事 | 次要 | P2 | 封面仍是单字占位符。 | Today hero 需要儿童一眼理解故事主题；单字占位不像正式故事书。 | 为每篇故事补儿童安全、无血腥战争细节的水彩/国风封面；hero 和列表共用同一视觉资产。 |

### 故事库

| 页面 | 严重度 | 优先级 | 问题 | 为何不合理 | 具体改进方案 |
|---|---|---:|---|---|---|
| 故事库 | 重要 | P1 | 故事顺序和系列进度不够显性。 | 首批内容是《三国演义》按原著顺序改写，Library 应强化“第 N 篇/继续读/已完成”，不只是 Level 筛选。 | 默认按故事顺序展示；卡片显示序号、系列名、状态和 Continue/Read again；Level 作为次级筛选。 |
| 故事库 | 重要 | P1 | 设计系统仍写 4 个顶级入口，但当前实现有 5 个 tab，包含 Words。 | IA 文档不一致会让验收、截图脚本、底部导航密度判断冲突。 | 产品层面二选一：确认 5 tab 为当前 MVP 并更新设计系统/mockup；或把 Words 移回阅读流，恢复 4 tab。 |
| 故事库 | 重要 | P1 | 筛选/空态需要页面语义化。 | 顶层 story list 的通用空态不能覆盖“筛选无结果”“内容未加载”“故事库为空”等不同情况。 | Library 单独定义 empty/error/loading 文案和 CTA；筛选无结果提供 “Clear filter”。 |
| 故事库 | 次要 | P2 | iOS Level filter 固定 `[1,2,3]`，Android 从内容派生。 | 内容扩展后 iOS 可能显示无效级别，双端不一致。 | iOS 从实际故事 level 派生筛选项，和 Android 保持一致。 |
| 故事库 | 次要 | P2 | 平板/大屏 grid 的最大宽度需要验收。 | 设计 tokens 要求大屏内容不过宽；故事卡过宽会破坏儿童阅读密度。 | grid 外层最大 960dp/等效宽度并居中；手机单列，平板两列。 |

### 阅读页（新版）

| 页面 | 严重度 | 优先级 | 问题 | 为何不合理 | 具体改进方案 |
|---|---|---:|---|---|---|
| 阅读页 | 阻断 | P0 | 新版阅读页缺有效截图验收证据。 | `31-reading-redesigned.png` 实际是 Today；现有 `23-reading.png`、`24-playing.png`、`24a-after-coachmark.png` 是旧版常驻控制区截图，不能证明新版 story-first 已达标。 | 重新采集新版阅读页截图：停止态、播放态、settings sheet、coachmark 后、底部滚动到 AI 卡片处。 |
| 阅读页 | 阻断 | P0 | 句子正文点击与 speaker icon 的音频语义不够清晰。 | TTS 规范要求默认不 autoplay，tap-to-listen 应明确；如果正文点击会按当前模式播放，儿童可能误触连续播放。 | 正文点击只做聚焦/解释，音频只由 speaker icon 触发；或把整句声明为音频控件并强制只播本句，提供 “Play sentence X of Y” 无障碍标签。 |
| 阅读页 | 阻断 | P0 | AI 入口使用 “Ask” 语义，像开放聊天。 | 项目红线是受控解释，不做开放聊天；“Ask” 会让儿童期待自由问答。 | 改为 “Explain this part / 解释这一段”；无自由输入框；卡片显示 “Only about this story / 只解释当前故事”。 |
| 阅读页 | 重要 | P1 | 底部固定栏和 inline AI 卡片存在遮挡风险。 | 旧截图显示正文尾部和 Ask 卡片被底部区域压住；新版也必须证明 scroll content 有足够 bottom padding。 | 阅读页 Lazy/Scroll content 增加 bottom dock + safe area padding；底部栏加上边界或阴影；AI 卡片必须完整可读可点。 |
| 阅读页 | 重要 | P1 | 设置 sheet 对儿童暴露过多听读术语。 | Read-along、Tap-to-listen、Auto-continue、Playback speed 对 5-8 岁儿童偏复杂，主任务是读故事。 | sheet 分组为 “Text / Listening”；默认只暴露拼音、字号、读给我听；速度/自动继续放入 More 或家长/设置。 |
| 阅读页 | 重要 | P1 | settings sheet 需要通过 360dp 和 1.3x 字体验收。 | 设计系统要求大字不裁切，所有控件至少 48dp；bottom sheet 容易在小屏上挤压 Done。 | sheet 内容可滚动，所有 row 触控区 >=48dp，Done 可见或可滚到；补小屏截图。 |
| 阅读页 | 重要 | P1 | iOS 一次渲染全部段落，Android 聚焦当前段落。 | 顶部进度和底部前进按钮按当前段落计算，iOS 全文滚动会让孩子不清楚当前读到哪一页。 | iOS 与 Android 对齐为当前段落模式；或改成全文模式时同步可见段落与进度。 |
| 阅读页 | 重要 | P1 | 拼音/ruby 排版在旧截图中有粘连风险。 | 5-8 岁儿童依靠拼音辅助，拼音和汉字不对应会直接影响阅读。 | 按字/词 ruby 单元控制最小间距；长词允许换行但不粘连；每行中文长度控制在舒适范围。 |
| 阅读页 | 次要 | P2 | 顶部只显示中文标题。 | 面向海外双语家庭，标题双语是定位的一部分。 | 顶栏保留中文时，在正文首屏显示英文副标题；或标题区域显示中英双行。 |

### 生字本 / 生字学习

| 页面 | 严重度 | 优先级 | 问题 | 为何不合理 | 具体改进方案 |
|---|---|---:|---|---|---|
| 生字本 | 阻断 | P0 | Word Book 已成为顶级 tab，但设计系统和 mockup 没有对应权威页面基线。 | 顶级页面缺基线会导致 IA、空态、复习规则、截图验收都无法统一。 | 更新设计系统和 mockup：Word Book = 跨篇生字汇总，含 Due/Learning/Known、来源故事、音频、无压力复习。 |
| 生字学习 | 重要 | P1 | Android 单篇 vocabulary 为空时仍可能进入 Quiz。 | 如果内容缺生字，孩子会从空学习页直接跳测验，流程语义不完整。 | `vocab.isEmpty` 时显示错误/返回阅读/继续测验的明确说明；不要让 Next 隐式进入 Quiz。 |
| 生字本 | 重要 | P1 | 没有到期词时，先显示 disabled “Start review”，再解释 no due。 | 儿童先看到不可点主按钮，会困惑为什么不能开始。 | dueCount=0 时隐藏 disabled 主按钮，改为 “Read today story” 或 “All caught up”；把 no due 状态放在统计卡后、操作前。 |
| 生字本 | 重要 | P1 | 顶级 Words 会增加底部导航密度。 | 5 个 tab 在 360dp 和 1.3x 字体下容易拥挤；是否值得顶级化需要产品决策。 | 若保留 5 tab，必须补 360dp/1.3x 截图和 label 不截断验收；否则将 Words 放回 Today/Reading 流。 |
| 生字学习 | 重要 | P1 | 双端返回路径不一致风险。 | iOS 生字页 close 固定回阅读页；如果从 Today summary 进入，返回阅读可能超出用户预期。 | 为 vocabulary 传入 entry source：Today 进入则回 Today，Reading 进入则回 Reading。 |
| 生字本 | 次要 | P2 | iOS 生字本缺少副标题，来源故事显示也需要更稳。 | 用户不清楚这是“到期复习”还是“全部生字”；Kotlin 集合桥接可能影响来源标题。 | iOS 补 `word_book_subtitle`；来源标题由 shared 提供 display DTO 或安全访问封装。 |

### 测验

| 页面 | 严重度 | 优先级 | 问题 | 为何不合理 | 具体改进方案 |
|---|---|---:|---|---|---|
| 测验 | 重要 | P1 | 未选答案时 Submit disabled，但页面没有显示选择提示。 | 5-8 岁儿童看到灰色按钮，不一定知道下一步要先选答案。 | 显示已有文案 `quiz_select_answer_prompt`；选择答案后隐藏。 |
| 测验 | 重要 | P1 | 答错反馈使用强错误色和 Close 图标。 | 陪读产品不应让答错像失败；儿童需要温和引导。 | 改用中性提示色和鼓励文案；提供 “Look back / 回到相关段落” 或 “Try the next one”。 |
| 测验 | 重要 | P1 | 完成页的分数和 streak 容易压过复述提示。 | 产品定位是理解故事，不是考试；分数不应成为唯一英雄反馈。 | 完成页主标题聚焦 “Story complete”；分数为次级，复述提示与鼓励同等突出。 |
| 测验 | 重要 | P1 | “Read again” 需要明确从第一段开始。 | mockup 要求重读从第一段进入；如果沿用保存进度，用户会误以为按钮失效。 | Read again 显式重置到 paragraph 0；如果不重置，则文案改为 “Continue reading”。 |
| 测验 | 重要 | P1 | 完成记录必须只由测验完成触发。 | TTS 规范明确听完音频不等于理解完成，不能自动算完成。 | 验收用例：音频播放结束只显示下一步 CTA；只有 quiz completion 才更新 story complete/streak。 |
| 测验 | 次要 | P2 | iOS 分数字符串存在硬编码格式。 | 双语资源后续无法调整格式或补单位。 | iOS 增加 `quiz_score_format`、parent quiz count 等本地化格式 key。 |

### 家长报告

| 页面 | 严重度 | 优先级 | 问题 | 为何不合理 | 具体改进方案 |
|---|---|---:|---|---|---|
| 家长报告 | 阻断 | P0 | Android 家长报告没有真正 gate，iOS gate 也只是继续按钮，双端不统一。 | 家长区展示儿童学习进度，儿童 App 至少需要成人意图确认；当前 Android 只是 notice，报告直接可见。 | 双端统一轻量 gate：长按、简单算术、家长 PIN 或确认手势；通过后再显示报告，未通过只显示说明。 |
| 家长报告 | 重要 | P1 | 隐私说明不够具体，且不一定首屏可见。 | COPPA/GDPR-K 风险需要清楚说明不收集儿童 PII；“No personal details” 太泛。 | 报告页和设置页统一隐私 note：不收集儿童姓名、生日、学校、照片、位置、联系方式；AI/analytics 匿名化。 |
| 家长报告 | 重要 | P1 | Android 可从报告点进故事，iOS 行为不一致。 | 家长看到进度后需要查看孩子读到哪里；双端应一致。 | iOS `StoryProgressRow` 增加 openStory 回调，或 Android 改成静态；建议保留可进入故事。 |
| 家长报告 | 重要 | P1 | 报告缺少家长可执行建议。 | 竞品机会强调家长端要从数据展示升级为行动计划；这也提升 seed user 感知价值。 | 增加一张本地模板建议卡：今晚复读哪篇、复习几个 due words、问哪个复述问题。 |
| 家长报告 | 次要 | P2 | 旧截图显示底部导航遮挡和隐私区靠后。 | 报告是家长信任页面，不能被底部栏裁切。 | 增加 bottom padding，隐私 note 提升到 metrics 后或固定可见区域。 |

### 设置

| 页面 | 严重度 | 优先级 | 问题 | 为何不合理 | 具体改进方案 |
|---|---|---:|---|---|---|
| 设置 | 阻断 | P0 | 普通设置页暴露 `AI backend base URL`。 | 这是开发配置，不适合儿童/家长 MVP；也会削弱“受控 AI 解释”的信任。 | 生产/QA 默认隐藏，移入 debug-only 或家长/开发者高级区；正式设置只展示 AI 解释边界和隐私说明。 |
| 设置 | 阻断 | P0 | 反馈表包含 parent contact，且入口可由儿童进入。 | 即使写了可选，儿童仍可能输入个人信息或家长联系方式，违反数据最小化原则。 | 反馈入口放到 parent gate 后；联系方式字段明确 “Parent email optional”，表单顶部和字段旁提示不要填写儿童姓名/学校/位置。 |
| 设置 | 重要 | P1 | Privacy row 不是完整隐私信息页。 | 儿童隐私和受控 AI 是核心信任点，一句摘要不足以支撑家长决策。 | 双端统一 Privacy sheet/page，列出数据最小化、无儿童 PII、AI 非开放聊天、匿名埋点、反馈注意事项。 |
| 设置 | 重要 | P1 | 设置页混合儿童设置、家长入口和开发设置，角色边界不清。 | 5-8 岁儿童会看到成人或技术概念；家长也难以判断哪些设置影响孩子。 | 分组调整为：Child reading、For grown-ups、About；For grown-ups 进入前复用 parent gate。 |
| 设置 | 重要 | P1 | 底部内容存在被 bottom nav 截断的截图证据。 | `04-settings.png` 中 AI backend 输入框被截断，影响可用性和完成度。 | 所有顶层滚动页面统一 bottom padding；设置页最后一项完全露出。 |

### 全局、一致性与验收证据

| 页面 | 严重度 | 优先级 | 问题 | 为何不合理 | 具体改进方案 |
|---|---|---:|---|---|---|
| 全局 | 阻断 | P0 | 截图集覆盖不完整且命名污染。 | `02-library.png`、`12-library.png`、`31-reading-redesigned.png` 等文件名与实际页面不符；新版阅读页、生字本、测验没有有效截图，无法做可靠验收。 | 重新采集并命名：`today.png`、`library.png`、`reading-stopped-redesigned.png`、`reading-playing.png`、`reading-settings-sheet.png`、`vocabulary.png`、`word-book.png`、`quiz-question.png`、`quiz-feedback.png`、`parent-report-gated.png`、`settings.png`、`onboarding.png`。 |
| 全局 | 阻断 | P0 | 截图中出现系统 launcher、黑屏和 ANR 弹窗。 | 这些截图不能作为 UX 验收证据，也提示启动/采集稳定性需要复查。 | 修复或规避截图采集稳定性问题；验收截图不得出现系统弹窗、launcher、黑屏。 |
| 全局 | 重要 | P1 | Chip 视觉高度为 40dp，低于 48dp 触控目标。 | Level filter、字号、状态筛选都是可点控件；设计系统要求触控 >=48dp。 | 保持视觉 40dp 也可以，但外层 clickable/touch target 必须 >=48dp；优先统一 token。 |
| 全局 | 重要 | P1 | 空态/加载/错误态需要页面化。 | 通用 “没有今日故事” 不能覆盖 Library、Word Book、Parent 等页面，会误导用户。 | `StoryStateContent` 接收页面级 empty/error 文案和 CTA；每页至少有重试或返回主路径。 |
| 全局 | 重要 | P1 | 5 tab 底部导航在 360dp/1.3x 下需要专项验收。 | 5-8 岁儿童触控和阅读需要更大目标，5 个英文标签容易拥挤或截断。 | 若保留 5 tab，补充 360dp、1.3x 字体、英文和中文截图；标签不得截断，触控区 >=48dp。 |
| 全局 | 次要 | P2 | 设计系统、roadmap、competitive doc 对当前实现状态有不一致处。 | 文档是多 agent 协作的权威输入，状态不一致会造成后续修复反复。 | 本轮修复后同步设计系统 IA、Word Book mockup、截图清单和 roadmap 当前状态。 |

## 汇总改进 Backlog

### P0：先修复，作为 MVP 验收门槛

| ID | 页面 | 可执行项 | 验收标准 |
|---:|---|---|---|
| P0-1 | 家长报告 | 双端实现统一 parent gate，报告数据通过后才可见。 | Android/iOS 进入 Parent 默认只显示 gate；通过 gate 后显示 metrics 和 story progress。 |
| P0-2 | 设置 | 隐藏或 gate `AI backend base URL`。 | 普通 Settings 不出现后端 URL 输入；debug/advanced 入口不进入儿童主路径。 |
| P0-3 | 设置 | 反馈入口移入成人区，联系方式字段加隐私保护。 | 未过 parent gate 不能进入 feedback form；表单明确禁止儿童 PII。 |
| P0-4 | 今日故事 | 修正 Today 选择策略和完成态 CTA。 | 未完成时 CTA = Start/Continue；完成时 CTA = Read again，下一篇作为 Up next；不会固定第一篇阻塞进度。 |
| P0-5 | 阅读页 | 收紧句子点击和 speaker icon 的音频语义。 | 正文点击不误触连续播放；speaker icon 只播本句；无障碍标签含句序。 |
| P0-6 | 阅读页 | AI 入口从 “Ask” 改为受控解释语义。 | 文案为 “Explain this part / 解释这一段”；没有开放输入；显示只围绕当前故事的边界。 |
| P0-7 | 生字本 | 确认 Word Book 是否为顶级 IA，并补设计基线。 | 设计系统、mockup、底部导航、截图脚本对 Words 入口一致。 |
| P0-8 | QA 截图 | 重新采集全页面有效截图。 | 每个评审对象至少 1 张有效截图；阅读页有停止态/播放态/sheet；没有 ANR、launcher、黑屏或错名。 |

### P1：端到端可跑后立即修复

| ID | 页面 | 可执行项 | 验收标准 |
|---:|---|---|---|
| P1-1 | 全局 | 所有顶层滚动页补 bottom nav + safe area padding。 | Today、Parent、Settings 等最后一项不被底部导航遮挡。 |
| P1-2 | 全局 | 所有 chip/clickable 控件满足 48dp 触控目标。 | 360dp 和 1.3x 字体下仍可点且不截断。 |
| P1-3 | 全局 | 页面化 loading/empty/error。 | Today、Library、Word Book、Parent 各有独立文案和恢复路径。 |
| P1-4 | 阅读页 | settings sheet 小屏可滚动并减少儿童侧高级术语。 | 360dp/1.3x 下 Done 可达；默认只显示儿童能理解的主设置。 |
| P1-5 | 阅读页 | iOS 与 Android 阅读段落模型对齐。 | 双端进度、底部按钮和可见段落一致。 |
| P1-6 | 故事库 | 强化顺序阅读和系列进度。 | 卡片显示第 N 篇、状态和 Continue/Read again；过滤后保持故事顺序。 |
| P1-7 | 生字学习 | 处理空 vocab 和 entry-source 返回路径。 | vocab 为空不直接跳 quiz；从 Today/Reading 进入时返回符合预期。 |
| P1-8 | 测验 | 增加选择提示、温和答错反馈、Read again 重置。 | 未选答案有提示；答错不使用惩罚式视觉；Read again 从第一段开始。 |
| P1-9 | 家长报告 | 隐私 note 具体化，并增加一条家长行动建议。 | 明确不收集儿童 PII；报告给出可执行的复读/复习/复述建议。 |
| P1-10 | 设置 | Privacy 入口变成完整 sheet/page。 | 双端可查看数据最小化、AI 边界、匿名埋点、反馈注意事项。 |

### P2：体验打磨

| ID | 页面 | 可执行项 | 验收标准 |
|---:|---|---|---|
| P2-1 | Onboarding | 改成更儿童友好的三卡片结构。 | 选中态不只依赖颜色，文案更像开始阅读而非配置表单。 |
| P2-2 | 今日故事 | 调整首屏标题层级和封面质量。 | “Today story” 成为主标题；封面不再是单字占位符。 |
| P2-3 | 阅读页 | 补英文副标题和拼音排版优化。 | 双语标题可见；拼音不粘连，字词对应清楚。 |
| P2-4 | iOS | 补齐格式化本地化字符串。 | quiz score、parent quiz count 等不再硬编码。 |
| P2-5 | 文档 | 同步 roadmap、design-system、mockup 状态。 | 多 agent 后续工作只引用一致的 IA 和验收清单。 |

## 建议的修复顺序

1. 先处理 P0-1 到 P0-6：儿童安全、隐私和阅读主交互。
2. 同步决策 P0-7：Words 是否顶级入口。该决策会影响底部导航、设计系统和截图脚本。
3. 完成 P0-8 后再做下一轮视觉验收，避免继续基于过期或错名截图判断。
4. P1 按主路径顺序修：Today -> Reading -> Vocabulary/Word Book -> Quiz -> Parent -> Settings。
