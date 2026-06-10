# pinyin-annotator — 拼音标注

**域:** 内容　**运行者:** codex　**优先级:** P0

## 角色
为每段正文生成与之**对齐**的标准汉语拼音（带声调），多音字按上下文语境选音。

## 输入
- `content/stories/<id>/story.json`（已含 `paragraphs[].text`）

## 输出
- 填充每个 `paragraphs[].pinyin` 与逐字 `paragraphs[].cells[].p`（二者一致）

## 约束
- 标准带调拼音（如 `hěn jiǔ`），词间空格，标点保留
- 与 `text` 严格对应，不增删内容；遵循 skill `story-json-format`

## 多音字校准（**内建于素材生产环节**，非事后人工审校）
拼音生成**不得只用 pypinyin 默认启发式**（对 V得C 助词、专名等易错）。生产流水线必须内建：
1. **神经多音字消歧**：`pypinyin` + **g2pW 后端**（`pypinyin_g2pw`）或 `g2pM`（CPU 友好），显著优于默认启发式。
2. **校准词表(overrides)**：可扩展覆盖表——专名（长坂 cháng、华容 huá…）、助词轻声（的/地/得/了/着 视语境）、V得C 结构助词 `得`→de、副词性 `地`→de 等。
3. **回归 lint**：`pipeline/validator` 的多音字 lint 作护栏（已存在）。
4. **发音验证**：批量 TTS 后用 **Qwen3-ASR 回环**转写 diff 原文，抓多音字误读，**回灌 overrides** 持续积累。

## 交叉验证
- `story-qa-validator` + 多音字 lint 校验每段 `pinyin`/`cells` 齐全且读音通过；ASR 回环抓误读。
