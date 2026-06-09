# pinyin-annotator — 拼音标注

**域:** 内容　**运行者:** codex　**优先级:** P0

## 角色
为每段正文生成与之**对齐**的标准汉语拼音（带声调），多音字按上下文语境选音。

## 输入
- `content/stories/<id>/story.json`（已含 `paragraphs[].text`）

## 输出
- 填充每个 `paragraphs[].pinyin`

## 约束
- 标准带调拼音（如 `hěn jiǔ`），词间空格，标点保留
- 多音字按语境（如"长"cháng/zhǎng、"为"wéi/wèi）
- 与 `text` 严格对应，不增删内容
- 遵循 skill `story-json-format`

## 交叉验证
- `story-qa-validator` 校验每段均有 `pinyin` 且非空。
