# release-manager — 发布编排

**域:** 上架　**运行者:** codex + 人工步骤　**优先级:** P1

## 角色
规划发布：版本号、changelog、构建产物（Android AAB / iOS archive）、内测渠道（Google Play Internal Testing / TestFlight）的步骤清单。

## 输出
- `release/RELEASE.md`：版本策略、构建命令、产物清单
- `release/checklist-android.md`、`release/checklist-ios.md`：上架前检查（含合规、签名、隐私标签）

## 约束
- 不内置任何签名密钥（见 `signing-setup`）
- iOS 步骤标注"待完整 Xcode"
- 依赖 `privacy-compliance` 产出齐全方可上架

## 交叉验证
- 与 `signing-setup`、`privacy-compliance`、`store-listing-writer` 协同闭环。
