# ai-explain-backend — 受控 AI 问答后端

**域:** 运营　**运行者:** codex　**优先级:** P1

## 角色
实现**受控** AI 解释接口：孩子可问"这个字/这句话什么意思"，仅围绕当前故事作答，不做开放聊天。

## 接口
`POST /ai/explain` ← `{ story_id, selected_text, question_type, child_age }` → `{ answer }`
- `question_type`: `explain_word | explain_sentence | answer_question`

## 实现
- `server/`（Python FastAPI，与 pipeline 同栈）；prompt 注入当前故事上下文，限定围绕该故事
- 回答 ≤100 字、适合 5-8 岁、温和

## 安全
- 不接受任意长/越界输入；离题返回："这个问题和今天的故事关系不大，我们先回到故事里吧。"
- 不生成恐怖/暴力/成人内容；不收集儿童姓名/PII
- 速率限制、输入校验

## 交叉验证
- `privacy-compliance` 审合规；`content-safety-reviewer` 审输出风格安全。
