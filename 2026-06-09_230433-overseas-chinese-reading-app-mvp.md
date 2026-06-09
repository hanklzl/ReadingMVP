# 海外华人儿童中文经典阅读 App MVP 开发计划

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** 先把一个可用于获取第一批种子用户的移动端 MVP 做出来，验证“海外华人/双语儿童通过中文经典故事进行 AI 陪读与阅读训练”的需求。

**Architecture:** 第一阶段采用“跨平台 App + 轻量后端 + 内容管理表”的方式，优先快速上线可测试版本。前端用 Flutter 或 React Native 均可；若没有既有技术栈，推荐 Flutter，一套代码覆盖 iOS/Android。后端优先 Supabase/Firebase 这类 BaaS，减少自建后台成本；AI 功能先做受控问答，不做开放聊天。

**Tech Stack:** Flutter 或 React Native；Supabase/Firebase；OpenAI/Anthropic/兼容模型 API；TTS 可先用系统朗读或云 TTS；内容初期用 Markdown/JSON 管理。

---

## 0. MVP 范围定义

### 目标用户
- 海外 5-8 岁华人儿童/双语儿童
- 家长希望孩子提高中文阅读兴趣与识字能力
- 孩子会听说一点中文，但读中文困难

### MVP 核心价值
- 每天 5-8 分钟读一个中文经典故事
- 有拼音、生字、朗读、简单理解题
- AI 只围绕当前故事解释生字、句子和问题
- 家长能看到孩子是否完成阅读

### 首版不做
- 不做海量书库
- 不做自由 AI 聊天
- 不做社区/评论/好友
- 不做复杂老师端
- 不做正式支付订阅，先手动邀请/邀请码内测
- 不做复杂游戏化，只做基础打卡

---

## 1. 推荐产品形态

### App 名称占位
- `Little Mandarin Classics`
- 中文名占位：`小小中文经典`

### 首版主导航
1. 今日故事
2. 故事库
3. 生字本
4. 家长报告
5. 设置

### 单篇故事结构
每篇故事控制在 300-600 字，适合 5-8 分钟完成。

每篇包括：
- 标题
- 封面图
- 年龄/难度
- 正文，支持拼音开关
- 朗读按钮
- 5-8 个生字词
- 3 道阅读理解题
- 1 个复述提示
- AI 提问入口，仅限当前故事

---

## 2. 首批内容计划

### 第一批 10 篇即可启动内测
1. 《石猴出世》
2. 《孙悟空学本领》
3. 《龙宫借宝》
4. 《大闹天宫》
5. 《精卫填海》
6. 《夸父追日》
7. 《龟兔赛跑》
8. 《狐狸和葡萄》
9. 《狮子和老鼠》
10. 《孔融让梨》

### 内容 JSON 字段建议
```json
{
  "id": "monkey-born",
  "title_zh": "石猴出世",
  "title_en": "The Birth of the Monkey King",
  "level": 1,
  "age_range": "5-8",
  "source_note": "Based on public-domain classic story, rewritten for children.",
  "cover_image": "assets/stories/monkey-born/cover.png",
  "paragraphs": [
    {
      "text": "很久很久以前，花果山上有一块大石头。一天，石头裂开了，跳出一只小猴子。",
      "pinyin": "hěn jiǔ hěn jiǔ yǐ qián，huā guǒ shān shàng yǒu yí kuài dà shí tou。yì tiān，shí tou liè kāi le，tiào chū yì zhī xiǎo hóu zi。"
    }
  ],
  "vocab": [
    {"word": "石头", "pinyin": "shí tou", "meaning": "stone", "example": "山上有一块大石头。"}
  ],
  "questions": [
    {
      "id": "q1",
      "type": "single_choice",
      "prompt": "小猴子从哪里跳出来？",
      "options": ["树上", "石头里", "河里"],
      "answer": "石头里",
      "explanation": "故事里说石头裂开了，跳出一只小猴子。"
    }
  ],
  "retell_prompt": "请你用一句话说说：小猴子是怎么来的？"
}
```

---

## 3. 开发阶段规划

## Phase 1：7 天做可点开的阅读 Demo

### Task 1: 创建移动端项目

**Objective:** 建立可运行的 App 工程。

**Files:**
- Create: `app/`
- Create: `app/lib/main.dart` 或 `app/src/App.tsx`
- Create: `app/assets/stories/`

**Steps:**
1. 选择 Flutter 或 React Native。
2. 创建项目。
3. 跑通 iOS Simulator / Android Emulator。
4. 提交初始 commit。

**Acceptance:**
- 本地能启动 App。
- 首页显示 `小小中文经典`。

---

### Task 2: 实现故事数据模型

**Objective:** 用本地 JSON 驱动故事展示，避免首版依赖后台。

**Files:**
- Create: `app/assets/stories/stories.json`
- Create: `app/lib/models/story.dart` 或 `app/src/types/story.ts`
- Create: `app/lib/services/story_repository.dart` 或 `app/src/services/storyRepository.ts`

**Steps:**
1. 定义 Story、Paragraph、Vocab、Question 数据结构。
2. 放入 2 篇样例故事。
3. 实现读取本地 JSON 的 repository。
4. 添加基础单元测试。

**Acceptance:**
- App 能读取本地 JSON。
- 故事库列表能显示 2 篇故事标题。

---

### Task 3: 实现故事库列表页

**Objective:** 用户可以看到可读故事列表。

**Files:**
- Create: `app/lib/screens/story_list_screen.dart` 或 `app/src/screens/StoryListScreen.tsx`

**UI:**
- 标题：`故事库`
- 卡片：封面、中文标题、英文标题、难度、预计阅读时间

**Acceptance:**
- 点击故事卡片进入阅读页。

---

### Task 4: 实现阅读页

**Objective:** 展示单篇故事正文。

**Files:**
- Create: `app/lib/screens/story_reader_screen.dart` 或 `app/src/screens/StoryReaderScreen.tsx`

**UI:**
- 标题
- 正文分段
- 拼音开关
- 字号调节，大/中/小
- 下一段/上一段或滚动阅读

**Acceptance:**
- 孩子能读完整篇故事。
- 拼音可开关。

---

### Task 5: 实现生字词模块

**Objective:** 每篇故事展示重点生字词。

**Files:**
- Create: `app/lib/widgets/vocab_card.dart` 或 `app/src/components/VocabCard.tsx`

**UI:**
- 生字
- 拼音
- 英文解释
- 例句

**Acceptance:**
- 阅读页底部显示本篇生字词。

---

### Task 6: 实现阅读理解题

**Objective:** 孩子读完后完成 3 道题。

**Files:**
- Create: `app/lib/screens/quiz_screen.dart` 或 `app/src/screens/QuizScreen.tsx`

**UI:**
- 单选题
- 选择后显示对/错和解释
- 完成后显示得分

**Acceptance:**
- 能完成题目并得到反馈。

---

## Phase 2：7-14 天做种子用户可用版本

### Task 7: 加入朗读功能

**Objective:** 提供故事朗读，降低低龄孩子阅读门槛。

**Approach:**
- MVP 可先用系统 TTS。
- 后续再换真人音频或高质量云 TTS。

**Files:**
- Create: `app/lib/services/tts_service.dart` 或 `app/src/services/ttsService.ts`

**Acceptance:**
- 点击朗读按钮后能读出当前段落或全文。
- 可以停止朗读。

---

### Task 8: 加入阅读完成记录

**Objective:** 记录孩子读过哪些故事。

**Approach:**
- MVP 先用本地存储。
- 正式内测再接 Supabase/Firebase 用户系统。

**Files:**
- Create: `app/lib/services/progress_service.dart` 或 `app/src/services/progressService.ts`

**Acceptance:**
- 完成测验后故事标记为已完成。
- 首页显示“已读 X 篇”。

---

### Task 9: 加入家长报告页

**Objective:** 让家长看到孩子进展，提升付费动机。

**UI:**
- 本周阅读篇数
- 已认识生字数
- 平均答题正确率
- 最近完成故事

**Acceptance:**
- 报告页从本地进度数据生成。

---

### Task 10: 加入受控 AI 问答

**Objective:** 孩子可以问“这个字什么意思”“这句话什么意思”，但不能自由闲聊。

**Approach:**
- 后端提供 `/ai/explain` 接口。
- 输入限定：story_id、selected_text、question_type。
- prompt 只允许围绕当前故事回答。
- 输出控制在 100 字内，适合 5-8 岁。

**Files:**
- Create: `server/src/routes/ai.ts`
- Create: `server/src/services/aiExplain.ts`
- Create: `app/lib/services/ai_service.dart` 或 `app/src/services/aiService.ts`
- Create: `app/lib/widgets/ask_ai_button.dart` 或 `app/src/components/AskAIButton.tsx`

**API shape:**
```json
POST /ai/explain
{
  "story_id": "monkey-born",
  "selected_text": "石头裂开了",
  "question_type": "explain_sentence",
  "child_age": 6
}
```

**Response:**
```json
{
  "answer": "这句话的意思是：大石头打开了一个口子。故事里的小猴子就是从里面跳出来的。"
}
```

**Safety:**
- 不接受任意长输入。
- 不回答与故事无关的问题。
- 不生成恐怖、暴力、成人内容。
- 不收集儿童真实姓名。

**Acceptance:**
- 阅读页可以选中文字并获取解释。
- 离题问题返回温和拒绝：`这个问题和今天的故事关系不大，我们先回到故事里吧。`

---

## Phase 3：14-30 天做可内测版本

### Task 11: 接入账号系统

**Objective:** 家长可以登录并跨设备保存进度。

**Approach:**
- Supabase Auth / Firebase Auth。
- 首版只支持邮箱 magic link 或 Google/Apple 登录。
- 不直接让孩子填写个人信息。

**User model:**
- parent_id
- display_name optional
- child_nickname optional
- child_age_range optional，如 `5-6`, `7-8`

**Acceptance:**
- 登录后阅读进度同步到云端。

---

### Task 12: 内容扩充到 10 篇

**Objective:** 达到可以让种子用户连续体验 1-2 周的内容量。

**Files:**
- Modify: `app/assets/stories/stories.json`
- Add: `app/assets/stories/<story-id>/cover.png`

**Acceptance:**
- 至少 10 篇故事。
- 每篇都有拼音、生字、3 道题。
- 至少 5 篇有封面图。

---

### Task 13: 加入邀请码机制

**Objective:** 控制早期用户规模，方便收集反馈。

**Approach:**
- 每个种子用户一个邀请码。
- 后台记录邀请码来源。

**Acceptance:**
- 无邀请码不能注册。
- 可统计每个渠道带来的用户。

---

### Task 14: 加入反馈入口

**Objective:** 方便种子用户反馈问题。

**UI:**
- 设置页：`给我们反馈`
- 字段：满意度、孩子年龄、遇到的问题、建议

**Acceptance:**
- 反馈进入后台表或发送到指定邮箱/飞书/Notion。

---

### Task 15: 埋点与核心指标

**Objective:** 能判断产品是否值得继续。

**Events:**
- app_open
- story_open
- paragraph_audio_play
- pinyin_toggle
- vocab_open
- quiz_start
- quiz_complete
- ai_explain_request
- story_complete
- parent_report_open

**Metrics:**
- D1/D7/D30 留存
- 每周阅读次数
- 单篇完成率
- 测验完成率
- AI 使用次数
- 家长报告打开率

**Acceptance:**
- 后台可看到每个事件统计。

---

## 4. 种子用户获取计划

### 第一批目标
- 100 个海外华人家庭
- 孩子年龄 5-8 岁
- 主要来自美国、加拿大、澳洲、新加坡

### 获取渠道
1. 海外华人家长微信群
2. 小红书/Instagram/TikTok 海外华人妈妈账号
3. 中文学校老师
4. 周末中文班家长群
5. 海外华人论坛/Reddit/Facebook Groups
6. 朋友转介绍

### 招募话术
> 我们正在内测一款给海外华人孩子用的中文经典故事 AI 陪读 App。适合 5-8 岁，会一点中文但不太愿意读中文的孩子。每天 5-8 分钟，有拼音、朗读、生字解释和简单问题。现在招募 100 个家庭免费试用 4 周，希望家长每周反馈一次。

### 种子用户筛选表单
字段：
- 国家/城市
- 孩子年龄
- 家里主要语言
- 孩子中文水平：听/说/读/写
- 是否上中文学校
- 家长最头疼的问题
- 是否愿意每周反馈
- 邮箱/微信/WhatsApp

---

## 5. 验证指标

### 100 个家庭内测目标
| 指标 | 及格线 | 优秀线 |
|---|---:|---:|
| 激活率 | 60% | 80% |
| D7 留存 | 30% | 50% |
| 4 周仍在使用 | 15% | 30% |
| 每周阅读次数 | 2 次 | 4 次 |
| 单篇完成率 | 40% | 70% |
| 家长愿意付费 | 5% | 15%+ |
| 可接受年费 | $29 | $49-$69 |

### 继续投入条件
满足任意 3 条即可继续：
- 100 个家庭中至少 50 个激活
- 至少 30 个家庭一周内读完 2 篇以上
- 至少 10 个家庭明确愿意付费
- 至少 5 位家长主动推荐给朋友
- 家长反馈“孩子愿意读中文了”

---

## 6. 开发优先级排序

### P0 必须有
- 故事列表
- 阅读页
- 拼音开关
- 生字词
- 朗读
- 阅读题
- 完成记录

### P1 种子用户阶段很重要
- 家长报告
- 反馈入口
- 受控 AI 解释
- 邀请码
- 基础埋点

### P2 暂后
- 支付订阅
- 老师端
- 班级管理
- 复杂游戏化
- 真人配音
- 大规模内容后台
- 多语言完整本地化

---

## 7. 30 天执行时间表

### 第 1 周
- 完成 App 工程
- 完成本地故事 JSON
- 完成故事库和阅读页
- 完成 2 篇样例故事

### 第 2 周
- 完成拼音、生字、题目、朗读
- 完成阅读进度
- 完成家长报告基础版
- 内容扩到 5 篇

### 第 3 周
- 接入受控 AI 解释
- 接入账号/云同步
- 接入反馈入口
- 内容扩到 10 篇

### 第 4 周
- 接入邀请码和埋点
- 做 TestFlight / Google Play Internal Testing
- 招募 30-50 个种子家庭
- 开始第一轮内测

---

## 8. 主要风险与应对

### 风险 1：孩子不愿意读
**应对:** 每篇短、图多、朗读强、完成后有鼓励。

### 风险 2：家长觉得内容太浅或太难
**应对:** 故事按 Level 1/2/3 分级，内测收集年龄和难度反馈。

### 风险 3：AI 回答不稳定
**应对:** AI 只解释当前文本，不做开放聊天；回答短；加安全过滤。

### 风险 4：内容制作慢
**应对:** 首批只做 10 篇；建立固定模板；先质量后数量。

### 风险 5：获客难
**应对:** 先不要投广告，优先海外华人家长社群和中文学校老师。

---

## 9. 建议下一步

立即执行顺序：
1. 选定技术栈：Flutter 或 React Native。
2. 确定首版是否只做 iOS TestFlight，还是 iOS + Android 同时做。
3. 写 2 篇完整故事样例。
4. 做阅读 Demo。
5. 找 5 个家长试读 Demo。
6. 根据反馈再扩展到 10 篇和 100 个家庭。

推荐决策：
- 如果你自己开发或团队熟悉前端：用熟悉技术栈。
- 如果从零选型：用 Flutter + Supabase。
- 如果想最快验证：先做 Web/PWA 或小程序式 H5，再包装成 App。

---

## 10. 最小版本验收清单

MVP 完成时必须能做到：
- [ ] 家长能安装测试版 App。
- [ ] 孩子能打开故事库。
- [ ] 孩子能读完一篇故事。
- [ ] 孩子能打开拼音。
- [ ] 孩子能听朗读。
- [ ] 孩子能查看生字解释。
- [ ] 孩子能完成 3 道题。
- [ ] 家长能看到已读记录。
- [ ] 用户能提交反馈。
- [ ] 团队能看到基础使用数据。

只要这些跑通，就可以开始种子用户验证。