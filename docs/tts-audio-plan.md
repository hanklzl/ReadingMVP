# 逐句语音 (TTS 大模型) — 选型与下一步计划

- 日期：2026-06-10
- 背景：阅读页要从"系统 TTS 整段读"升级为**TTS 大模型逐句生成 + App 逐句播放高亮**，并把音频生产并入内容流水线。
- 当前：链路实现已作为后台 codex 任务进行中（可插拔 provider + mock 默认离线可跑）。本文定"接哪个真实 TTS 大模型"。

## 关键判断：按"批量预生成 + 阅读发音准确"选，不是按价格

- 音频是**离线批量预生成**（不是用户实时合成），内容集小且固定（现 10 篇，目标 50-100 篇）。
- 一篇正文 300-600 字 → 10 篇 ≈ 5K 字，100 篇 ≈ 50K 字。即便按 ElevenLabs 顶配 ~$300/M 字，100 篇也 **< $15**；按 OpenAI/Azure ~$15/M 字，100 篇 **< $1**。
- **结论：成本可忽略，决策因子是「中文童声/讲故事音色」+「发音/声调准确」+「逐句/逐字时间戳(给高亮)」。**

## Provider 对比（面向"批量·中文·儿童阅读"）

| 方案 | 中文童声/音色 | 发音·声调 | 时间戳(高亮) | 接入/许可 | 定位 |
|---|---|---|---|---|---|
| **Azure Neural TTS** | 有真正的 **zh-CN 童声**(如 Xiaoshuang 童声) | 准、SSML 可控停顿 | **word boundary** 事件 | 托管 API、有免费额度、便宜 | **推荐默认(MVP)** |
| MiniMax Speech-02 | 强中文、表现力好 | 好 | 一般 | 托管 API(~$100/M HD) | 质量升级 |
| ElevenLabs v3 (2026) | 最自然、可加 [excited] 等情感标签、童声库 | 好 | 对齐 API | 托管(premium) | 顶配/旁白级 |
| OpenAI tts (gpt-4o-mini-tts) | 简单便宜，中文童声非强项 | 尚可 | 弱 | 托管(~$15/M) | 兜底/最省事 |
| **自托管开源**：CosyVoice 3 (Apache-2.0) / Qwen3-TTS / Fish Speech 1.5 | 可**克隆一个统一"温暖讲故事"音色**、支持情感/方言 | 顶级(Qwen3-TTS WER SOTA) | 可出对齐 | 自托管、$0 边际 | **规模化/长期最佳** |

## 推荐

- **provider 抽象保持可插拔**（codex 正在建）：`mock`(离线) + 真实大模型适配器。
- **默认真实 provider = Azure zh-CN 童声**：便宜、发音准、**有真正童声**、有 word-boundary 便于将来逐字高亮；最快"把链路接通真实音频"。
- **质量升级 = ElevenLabs v3 / MiniMax Speech-02**（旁白级表现力）。
- **规模化 = 自托管 CosyVoice 3 / Qwen3-TTS**：克隆**同一个温暖讲故事音色**贯穿全系列，边际 $0、可控、可离线再生。
- 真人配音（路线图 C1）留作旗舰级质量选项；TTS 大模型是可规模化的默认。

## 高亮路线

- **现在**：真实 Qwen 生成默认按**整段合成 → forced align → 切成逐句 wav**，App 仍按逐句资源播放并做逐字高亮。
- **下一步**：用 word-boundary/对齐时间戳做**逐字"卡拉OK"高亮**（Azure word boundary 或对齐 API；自托管可出 phoneme/word 对齐）。与现有逐字拼音 ruby 天然契合。

## 下一步计划（TTS 专项）

1. ✅ 链路打通（句切分 + 可插拔 provider + mock + 逐句播放高亮）。
2. ✅ Qwen 长期一致性路径：默认按段合成、写入 `ttsProfile`、按 forced alignment 切句，并校验 `audio.json.durationMs` 与 wav header 一致。
3. 接 **Azure 适配器**，配 key，批量生成真实童声音频，验收音质/发音。
4. **音色 A/B**：Azure 童声 vs 自托管 CosyVoice/Qwen 克隆音色，选定系列统一音色。
5. 随内容扩展批量生成；评估自托管以控成本/统一音色。

## 与总体路线图的关系

本项属路线图（feature-roadmap.md）阶段 C「内容与体验深化 / 真人或高质量配音」的提前落地；可与阶段 A 留存功能、阶段 B 账号/付费并行推进（不同子系统、不抢同一 Gradle 工程时错峰）。

## Sources

- TTS 选型/质量：[Best TTS providers 2026 (Coval)](https://www.coval.ai/blog/best-text-to-speech-providers-in-2026-how-to-choose-(and-why-vendor-benchmarks-lie)/) · [Best open-source TTS 2026 (Neosophie)](https://neosophie.com/en/blog/20260317-tts) · [Qwen3-TTS report](https://arxiv.org/html/2601.15621v1) · [Fish Audio blind test](https://fish.audio/blog/blind-tts-provider-comparison-2026/)
- 定价：[TTS API pricing 2026 (DEV)](https://dev.to/leanvox/tts-api-pricing-in-2026-i-went-through-every-provider-so-you-dont-have-to-bem) · [TTS cost calculator](https://ttscost.com/) · [ElevenLabs API pricing](https://elevenlabs.io/pricing/api)
- 中文/童声：[ElevenLabs Mandarin TTS](https://elevenlabs.io/text-to-speech/mandarin-chinese) · [Alibaba Model Studio Qwen-TTS/CosyVoice](https://www.alibabacloud.com/help/en/model-studio/realtime-tts-user-guide)
