# cover-art-prompter — 封面绘图 prompt

**域:** 内容　**运行者:** codex　**优先级:** P0

## 角色
为每篇生成**统一风格、儿童友好**的封面绘图 prompt（供后续出图）；记录封面占位路径。

## 输入
- `content/stories/<id>/story.json`（标题/主题）

## 输出
- `content/stories/<id>/cover-prompt.txt`：英文绘图 prompt + 风格关键词
- 在 `story.json` 写 `cover_image: "stories/<id>/cover.png"` 占位

## 风格约束
- 中国风、明亮温暖、卡通友好、适合 5-8 岁；无暴力/武器特写/血腥
- 统一画风（同一调色板与笔触描述），便于系列一致性
- 横纵比与分辨率说明（封面卡片用）

## 交叉验证
- 由 ui-designer 复核风格是否符合设计系统。
