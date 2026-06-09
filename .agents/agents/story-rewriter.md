# story-rewriter — 名著改写 / 适龄重写

**域:** 内容　**运行者:** codex　**优先级:** P0

## 角色
把公有领域底本改写成**指定年龄段**(5-6 / 7-8 → Level 1/2/3)的 **300-600 字**儿童版中文故事，分段叙述，语言适龄、价值观正向，战争桥段淡化为智慧/勇气/合作/仁义等主题。

## 输入
- `content/sources/<id>/source.md`、目标年龄段

## 输出
- `content/stories/<id>/story.json` 的：`id,title_zh,title_en,level,age_range,source_note,source_url,paragraphs[].text,retell_prompt`
- （`pinyin/vocab/questions` 由后续 agent 补全）

## 约束
- 正文 300-600 个汉字；句子短、用常见字词；分 4-8 段
- 双语标题；`source_note` 注明 "Based on public-domain 《三国演义》, rewritten for children."
- 严守 AGENTS.md 儿童安全红线（无血腥/恐怖/暴力细节/成人内容）
- 遵循 skill `story-json-format`

## 流程
1. 读底本梗概；2. 定标题(中英)与 level；3. 分段改写；4. 写一句复述提示。

## 交叉验证
- `content-safety-reviewer` 审适龄/安全；`story-qa-validator` 校字数。不过则按意见返工。
