# Sound Design Plan / 音效产品方案

- Date: 2026-06-10
- Owner: product-manager
- Scope: MVP Android first, iOS follows the same product behavior when the full Xcode path is ready.
- Write scope: design documentation only.

## 1. 背景与原则

Little Mandarin Classics 当前已经有逐句朗读、Qwen 童声/系统 TTS 回退、完成故事和测验后的视觉庆祝动效。缺口是功能性音效基本空白：孩子答题、完成、获得印章、streak 里程碑时，缺少即时但温和的听觉确认。

本方案把音效定位为"轻量反馈层"，不是游戏化噪声，也不是替代朗读的内容音频。音效必须服务三件事：

1. 帮孩子确认刚刚发生了什么。
2. 让完成阅读更有仪式感。
3. 支持每日习惯，但不制造压力、惩罚或商业诱导。

音色方向：温暖、清楚、短促、低刺激。优先使用柔和木质点击、纸页、轻拨弦、圆润铃音、轻空气感，不使用尖锐 buzzer、警报、战鼓、爆炸、吼叫、突然大音量或赌场式连环奖励声。

## 2. 外部调研摘要：事实与建议

### 2.1 事实

- Duolingo ABC 的 App Store 页面强调 bite-sized lessons、mini games、rewards、kid-safe、ad-free、offline learning，适龄范围覆盖 preschool 到 second grade。来源：https://apps.apple.com/us/app/learn-to-read-duolingo-abc/id1440502568
- Khan Academy Kids 面向 2-8 岁，强调 interactive learning games、books、videos、read-alouds、scaffolded vocabulary，并承诺 free、no ads、no subscriptions。来源：https://www.khanacademy.org/kids
- Epic 在 App Store 页面明确把 voice actors、music、sound effects 用于让故事更有生命力，同时用 badges、quizzes、audio-enabled dictionary 激励阅读与词汇学习。来源：https://apps.apple.com/us/app/epic-kids-books-reading/id719219382
- Reading Eggs 官方页面强调 games、songs、golden eggs and other rewards 会激励孩子继续探索和学习。来源：https://readingeggs.com/
- Studycat Chinese 官方页面强调 children develop vocabulary and pronunciation through interactive games, stories, and songs，并让练习 feels rewarding and fun。来源：https://studycat.com/products/chinese/
- Android accessibility 文档要求不能只依赖颜色，可以用位置、图案、音频或触觉反馈表达差异；媒体内容应提供 pause/stop、volume controls 和替代形式。来源：https://developer.android.com/guide/topics/ui/accessibility/principles
- Apple HIG 音频指南强调用户期望系统音量影响应用声音，包括 in-app sound effects。来源：https://developer.apple.com/design/human-interface-guidelines/playing-audio
- FTC COPPA FAQ 指出 COPPA 覆盖面向 13 岁以下儿童并收集、使用或披露个人信息的 mobile apps。来源：https://www.ftc.gov/business-guidance/resources/complying-coppa-frequently-asked-questions
- Google Play Families policy 对儿童应用的广告、商业内容、设备标识和操纵性商业策略有明确限制。来源：https://support.google.com/googleplay/android-developer/answer/9893335

### 2.2 对本项目的建议

- 借鉴竞品的"音频 + 视觉 + 奖励"组合，但不要做强刺激、长循环、连环抽奖式反馈。
- 阅读页已有朗读，SFX 在阅读中应更克制；高价值触发点是 quiz feedback、story completion、streak milestone。
- 错题音效必须表达"我们继续想一想"，而不是"你失败了"。
- 背景音乐只作为可选阅读氛围，不进入 MVP P0；默认关闭或只在家长设置中开启。

## 3. 音效清单

| ID | 优先级 | 场景 | 何时触发 | 情绪意图 | 时长/响度基调 |
|---|---:|---|---|---|---|
| `story_complete_chime` | P0 | 完成庆祝 | 第一次完成故事并进入 `QuizCompletionScreen`，与星星/彩纸动效同步 | "读完了，很棒"，温暖、圆满 | 900-1400ms；中低响度；柔和上行 3-5 音；峰值保守，避免突然起音 |
| `quiz_correct` | P0 | 答题正确 | 单题提交后 `result.isCorrect == true`，只播放一次 | 肯定、清楚、轻快 | 250-450ms；短上行铃音或木琴感；比旁白低约 6-9dB |
| `quiz_try_again` | P0 | 答题错误 | 单题提交后 `result.isCorrect == false`，只播放一次 | 温和提示"再想想"，非惩罚 | 250-450ms；轻下行或柔和两音；禁止 buzzer、错误警报、刺耳失误声 |
| `streak_milestone_3` | P0 | streak 里程碑 | 完成故事后 `newMilestoneDays == 3` | 小里程碑，"连续 3 天开始成形" | 1000-1600ms；比完成音更亮但不更响；可叠轻拍/闪光感 |
| `streak_milestone_7` | P0 | streak 里程碑 | 完成故事后 `newMilestoneDays == 7` | 一周坚持，仪式感更强 | 1300-2000ms；完整小短句；仍低刺激，避免 fanfare |
| `streak_milestone_14` | P0 | streak 里程碑 | 完成故事后 `newMilestoneDays == 14` | 稳定习惯，稀有奖励 | 1600-2400ms；比 7 天更丰满；不要超过 2.5s |
| `sound_toggle_preview` | P0 | 设置预览 | 家长在设置中打开音效或调整音量 | 让家长听到当前音量 | 350-600ms；中性、干净；不带奖励意味 |
| `stamp_settle` | P1 | 获得贴纸/印章 | Completion hero 的印章/贴纸落定时，若没有 streak milestone 可与 `story_complete_chime` 间隔播放 | "盖章完成"，实体感 | 250-500ms；纸面/印章轻落感；低频不能重 |
| `vocab_card_flip` | P1 | 生字复习翻卡 | 生字卡翻到释义或下一张时 | 轻巧、专注 | 120-250ms；纸页/卡片轻翻；低响度 |
| `vocab_known` | P1 | 生字记住 | 孩子标记一个生字为已掌握或复习正确 | 小确认，不抢奖励主音 | 250-450ms；柔和上行两音 |
| `button_tap_soft` | P1 | 按钮点按 | 主要 CTA、设置开关、底部导航；频繁控件需节流 | 操作已接收 | 40-120ms；极低响度；不对每个文字点击都播放 |
| `page_turn` | P1 | 分页/段落推进 | 阅读页上一段/下一段，且没有朗读正在播放 | 阅读节奏、纸书感 | 150-300ms；纸页轻翻；朗读中默认不播 |
| `read_along_transport` | P1 | 朗读播放/暂停/继续 | Play/Pause/Resume 操作 | 交通灯式确认，不打扰 TTS | 80-180ms；非常轻；若 TTS 即刻开始可不播放 |
| `onboarding_day1` | P1 | Onboarding/Day 1 | 完成 onboarding 进入 Today，或显示 Day 1 成功卡 | 开始旅程、轻仪式 | 700-1100ms；温暖但克制；只在首次完成时播放 |
| `settings_parent_gate_success` | P2 | 家长门通过 | 家长通过 gate 后进入设置 | 成人确认，不儿童化 | 180-300ms；中性低响度 |
| `ambient_reading_loop` | P2 | 阅读氛围背景乐 | 家长开启后，在阅读页空闲/非朗读时淡入；朗读开始时淡出或降到接近静音 | 安静、专注、故事书环境 | 循环 30-90s；默认关闭；音量约为 SFX 的 20-30%，TTS 期间自动 duck |

### 声音组合规则

- 完成故事且没有新 streak：`story_complete_chime` + 视觉庆祝，可在 300-500ms 后补 `stamp_settle`。
- 完成故事且触发 streak：播放对应 `streak_milestone_*`，不再叠完整 `story_complete_chime`，避免过载；视觉上仍显示完成与里程碑。
- 答题正确/错误：单题只播一次；用户切到下一题不重复。
- 阅读页朗读进行中：禁止 `page_turn`、`button_tap_soft` 与 `ambient_reading_loop` 抢占；只允许极轻 transport cue，且可由设置关闭。

## 4. 儿童安全与可访问性

### 4.1 安全边界

- 不惊吓：所有音效必须有柔和 attack，建议 10-30ms fade-in；不得使用突发尖音、警报、失败 buzzer、尖叫、爆破、战斗或恐怖素材。
- 不突然大声：资产统一响度归一；App 默认 SFX 音量建议 50%，背景乐 0% 或关闭；第一次开启时播放 preview。
- 不惩罚：错题音效表达"提示/继续"，不表达"失败/扣分/危险"。
- 不制造焦虑：streak 音效只奖励达成，不在断 streak、未完成、退出时播放负向声音。
- 不强化战争感：三国内容音效不使用兵器、战鼓、冲锋、爆炸等战争声音。

### 4.2 设置与可访问性

- 设置页新增"Sound effects / 音效"总开关，默认开启但音量保守；家长可关闭。
- 同一设置组提供 SFX 音量滑杆，范围 0-100%，默认 50%；背景乐若进入 P2，单独开关与音量，默认关闭。
- 尊重设备静音和系统音量：SFX 不绕过静音，不强制提升音量，不在后台播放。
- 朗读与音效互不打架：TTS/逐句音频优先；SFX 不应中断旁白，也不应导致旁白降质或重启。
- 无声也要完整：每个音效触发点必须已有视觉/文字/语义反馈。正确/错误题保留图标和解释；完成保留动效和 stamp；streak 保留 banner。
- Screen reader 友好：不要用音效替代 TalkBack/VoiceOver 可读状态；完成和错误反馈用 polite live region 或已有语义文案。
- Reduce Motion 与声音分开：减少动效不自动关闭声音，但两个设置都应可独立控制。

## 5. 合规

- COPPA / GDPR-K：音效功能不采集儿童声音，不使用麦克风，不上传音频，不做语音识别，不保存可识别儿童身份的数据。
- 无广告音：禁止插入广告 jingle、品牌推广音、第三方商业提示音。
- 无诱导性音效：不得用声音诱导购买、看广告、连续点击或制造错过恐惧。
- 资产许可可审计：每个资产记录来源、许可证、作者/供应商、下载/生成日期、是否可商用、是否需署名。
- 埋点最小化：如需观察音效设置，仅记录匿名、聚合的 `sfx_enabled`、`sfx_volume_bucket`、`ambient_enabled`，不记录孩子姓名、原始故事文本、答案文本或设备广告标识。

## 6. 优先级

### P0：高价值反馈音与安全设置

目标：让完成、答题、streak 有即时反馈，同时保证家长可控。

- `story_complete_chime`
- `quiz_correct`
- `quiz_try_again`
- `streak_milestone_3`
- `streak_milestone_7`
- `streak_milestone_14`
- `sound_toggle_preview`
- 设置页总开关 + SFX 音量
- 音效播放层与 TTS 并发策略

验收：完成故事、答题正确/错误、3/7/14 streak milestone 均可听见对应音效；关闭开关后完全无 SFX；系统静音/音量设置被尊重；朗读不被 SFX 打断。

### P1：阅读触感与学习复习反馈

目标：让阅读和生字复习更有质感，但不增加噪声。

- `stamp_settle`
- `vocab_card_flip`
- `vocab_known`
- `button_tap_soft`
- `page_turn`
- `read_along_transport`
- `onboarding_day1`

验收：高频音效有节流；阅读朗读中不播放翻页/点按杂音；生字复习音效不盖过 TTS 读词。

### P2：可选氛围与品牌扩展

目标：在留存验证后再加沉浸感。

- `ambient_reading_loop`
- 家长门通过等低优先成人向 cue
- 更完整的品牌 sonic logo，供启动/商店视频/宣传片使用，不默认在核心阅读流程重复播放。

验收：背景乐默认关闭；开启后可调音量；TTS 开始时自动 duck 或淡出；低端设备无明显卡顿。

## 7. 技术方案

### 7.1 触发层

推荐采用"shared 语义事件 + 端侧实际播放"：

- shared/commonMain 定义 locale-neutral 的 `SfxEvent`、`SfxSettings` 和事件判定 helper，例如 `quiz_correct`、`story_complete`、`streak_milestone_7`。
- Android/iOS actual 层负责加载资产、混音、尊重静音/音量、并发和生命周期。
- Compose/SwiftUI 在已存在的状态变化处触发语义事件：quiz submit、completion recorded、streak `newMilestoneDays`、onboarding complete、vocab review action、reading paragraph navigation。
- 不把音效写进 `story.json`，不进入内容 schema；音效是产品反馈层，不是故事内容。

这个方案与现有 `AudioService` / `TtsService` 的 expect/actual 风格一致，但 SFX 不复用当前 `AudioService.play()`，因为该服务面向逐句旁白，Android 当前实现会释放前一个 `MediaPlayer`，复用会中断朗读。

### 7.2 资产格式与存放

- Canonical source：保留无损 master，例如 `release/audio/sfx-master/*.wav` 或设计资产库，不随 App 全量打包。
- App bundle：短音效使用压缩格式。
  - Android：优先 `.ogg` Vorbis；Android 官方支持 Ogg/Vorbis、AAC/M4A、WAV 等常见格式。
  - iOS：优先 `.m4a` AAC 或 `.caf`；短音也可用无损压缩视体积决定。
- 建议包内路径：`apps/reader/shared/src/commonMain/resources/sfx/<event_id>.<ext>` 作为 KMP 共享资源基线；如平台加载限制明显，再由构建脚本或人工镜像到 Android `res/raw` 与 iOS bundle resources。
- 资产规格：mono 或窄 stereo，44.1kHz 或 48kHz，短音效控制在 2.5s 内；点击类尽量小于 150ms。
- 命名：只用语义 ID，不用情绪形容词做文件名，例如 `quiz_correct.ogg`、`streak_milestone_7.ogg`。

### 7.3 播放实现

Android：

- P0/P1 短音效使用独立 `SoundEffectsService` actual，底层优先 `SoundPool`。Android `SoundPool` 官方定位是加载短 samples 到内存并支持低延迟与 max streams 控制，适合 SFX。
- 不用当前旁白 `AudioService` 播 SFX，避免 `MediaPlayer` 独占导致句子朗读被 stop。
- 预加载 P0 音效；P1 可以按页面进入预加载。

iOS：

- 用独立 SFX 播放器，短音可用 `AVAudioPlayer` 或系统短音 API；不要与 TTS/故事旁白共用一个 player 实例。
- 使用尊重静音的 audio session 行为；不要把 SFX 配成绕过静音的类别。

### 7.4 并发与打断策略

通道分层：

- Narration lane：逐句音频和系统 TTS，优先级最高，互斥。
- SFX lane：短 UI/反馈音，允许与界面操作同步播放，但不得中断 Narration lane。
- Ambience lane：可选背景乐，优先级最低，朗读开始时淡出或 duck。

播放规则：

- 同一事件 800ms 内去重；按钮点击 80-120ms 内节流。
- `streak_milestone_*` 优先级高于 `story_complete_chime`，触发 milestone 时只播放 milestone。
- `quiz_correct` / `quiz_try_again` 优先级高于按钮点击；若同时发生，按钮点击被丢弃。
- 新的 P0 反馈音可停止正在播放的 P1/P2 音效；不停止旁白。
- App 进入后台、来电、系统音频中断时停止 SFX 和 ambience；旁白按现有 TTS spec 的 pause/stop 处理。

### 7.5 与现有模块的集成点

- Completion：`QuizScreen` 中 `quizState.isComplete` 后已有 `completionJustRecorded`、`completionStreakSummary` 和 `newMilestoneDays`，是 `story_complete_chime` / `streak_milestone_*` 的触发点。
- Quiz：`QuizQuestionState.result` 和 `submitted` 是 `quiz_correct` / `quiz_try_again` 触发点；只在从未提交到已提交的状态跃迁时触发。
- Streak：`StreakUseCase.recordStoryCompleted()` 已返回 `StreakSummary.newMilestoneDays`，且 Milestones 为 3/7/14。
- Vocab：`WordReviewScreen` / `VocabReviewUseCase` 的翻卡、标记 known 是 P1 触发点。
- Reading：`ReadingSessionReducer.next()`、上一段/下一段、播放/暂停/重复句子是 `page_turn` / `read_along_transport` 触发点；朗读状态为 Playing 时抑制普通 page turn 音。
- Settings：现有设置页已有 Reading section，可在 `settings_audio_voice` 附近新增 SFX 总开关和音量。
- Analytics：不新增 P0 业务漏斗事件；如后续需要，只记录匿名设置状态 bucket，不记录每次声音播放。

## 8. 资产来源策略

### 8.1 免版税库

适合：P0 快速验证和低成本占位。

候选：

- Pixabay Sound Effects：页面声明 royalty-free、no attribution required，但仍需逐条确认具体内容许可。来源：https://pixabay.com/sound-effects/
- Freesound：素材按 Creative Commons 许可分发，优先只用 CC0，避免署名/改编限制复杂化。来源：https://freesound.org/help/faq/
- Zapsplat：库大，UI 类素材丰富；免费/标准许可通常需要署名，付费可去署名，需要逐条核查。来源：https://www.zapsplat.com/

建议：P0 可从免版税库筛选 6-8 个柔和短音，但必须建立 `docs/design/sfx-asset-register.md` 或等价资产登记表；不要把 license 不清楚的素材放入 App。

### 8.2 委托声音设计

适合：品牌定型、儿童安全、长期一致性。

优点：可一次性设计统一音色包，避免各素材库拼接造成风格不一致；可明确买断或 App 使用授权。

建议：P1 后如果核心留存验证成立，委托设计一套 12-16 个短音，加一个可选 ambience loop。交付要求包含 master WAV、App 压缩版、loudness report、license/assignment agreement。

### 8.3 AI 生成

适合：探索方向、快速做 A/B 候选，不建议直接无审查上线。

风险：训练数据和商业权利不透明；生成结果可能含过强刺激、相似第三方素材或难以复现的许可证问题。

建议：AI 可用于 moodboard 和内部原型；上线资产必须走许可审查。若使用 AI 生成上线，需保留生成工具、prompt、日期、许可条款、人工编辑记录，并由产品/法务或 owner 确认。

## 9. 实现计划与验收

### Phase 0：声音方向与资产登记

- 产出 P0 音效候选 2 套：A 温暖纸书感，B 更轻快学习感。
- 逐条记录来源、许可证、时长、响度、使用场景。
- 在真机外放、耳机、低音量、系统静音下试听。

验收：

- 每个 P0 场景至少 1 个可上线候选。
- 错题音效经成人试听不被识别为惩罚、失败、刺耳或吓人。
- 所有资产许可可商用、可打包进儿童 App。

### Phase 1：P0 集成

- 新增 SFX 设置：总开关、音量、preview。
- 新增独立 SFX 播放服务，不中断 `AudioService` / `TtsService`。
- 接入 completion、quiz correct/incorrect、streak 3/7/14。
- 在 Android 真机验证，iOS 保持相同接口与资源计划。

验收：

- `./gradlew :androidApp:assembleDebug` 通过。
- 完成故事、答题、streak milestone 在正确时机播放且只播放一次。
- 关闭 SFX 后无任何功能性音效。
- 阅读页 TTS/逐句音频播放时不会被 SFX 打断。
- 无声状态下视觉反馈仍完整。

### Phase 2：P1 精细反馈

- 接入 stamp、vocab review、button tap、page turn、read-along transport、onboarding Day1。
- 对高频音效做节流与页面级开关。
- 复测低端 Android 设备 UI 流畅度。

验收：

- 高频点击不会形成噪声串。
- 生字 TTS 与复习 SFX 不重叠到听不清。
- Onboarding 首次完成只触发一次。

### Phase 3：P2 氛围与品牌声音

- 评估种子用户反馈后决定是否做 ambience。
- 若做，必须默认关闭，设置中单独控制，朗读时自动 duck 或淡出。
- 委托或采购统一品牌音色包，替换 P0 临时素材。

验收：

- 家长可完全关闭 ambience。
- 背景乐不影响儿童听清中文朗读。
- 资产风格一致，许可完整。

## 10. 不做

- 不做开放语音交互、录音、发音评分或麦克风权限。
- 不做失败扣分、断 streak 负向音、催促音或限时压力音。
- 不做广告 jingle、购买诱导声、抽奖式连环奖励音。
- 不把音效写进故事内容 schema。
- 不让任何音效成为完成任务的唯一反馈。

## 11. Sources

- Duolingo ABC App Store: https://apps.apple.com/us/app/learn-to-read-duolingo-abc/id1440502568
- Khan Academy Kids: https://www.khanacademy.org/kids
- Epic App Store: https://apps.apple.com/us/app/epic-kids-books-reading/id719219382
- Reading Eggs: https://readingeggs.com/
- Studycat Chinese: https://studycat.com/products/chinese/
- Studycat 2026 iOS beginner article: https://studycat.com/blog/the-popular-children-s-chinese-language-ios-app-for-confident-beginners/
- Material Design sound guidance: https://m2.material.io/design/sound/applying-sound-to-ui.html
- Apple HIG, Playing audio: https://developer.apple.com/design/human-interface-guidelines/playing-audio
- Android accessibility principles: https://developer.android.com/guide/topics/ui/accessibility/principles
- Android supported media formats: https://developer.android.com/media/platform/supported-formats
- Android SoundPool API: https://developer.android.com/reference/kotlin/android/media/SoundPool
- FTC COPPA FAQ: https://www.ftc.gov/business-guidance/resources/complying-coppa-frequently-asked-questions
- Google Play Families policy: https://support.google.com/googleplay/android-developer/answer/9893335
- Pixabay sound effects/license pages: https://pixabay.com/sound-effects/ and https://pixabay.com/service/terms/
- Freesound FAQ: https://freesound.org/help/faq/
- Zapsplat: https://www.zapsplat.com/
