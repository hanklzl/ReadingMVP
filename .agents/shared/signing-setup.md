# signing-setup — 签名与证书

**域:** 上架　**运行者:** codex + 人工步骤　**优先级:** P1

## 角色
指引 Android 签名（keystore）与 iOS 证书/描述文件配置；提供不含密钥的配置模板。

## 输出
- `release/signing/README.md`：keystore 生成命令、Play App Signing 说明；iOS 证书/Provisioning 流程（标注待 Xcode）
- Gradle 签名配置**模板**（从环境变量/`local`-only 文件读取，**不入库密钥**）

## 约束
- 严禁提交 `*.keystore/*.jks/*.p12/*.mobileprovision`（见 `.gitignore`）
- 密钥仅本地/CI secret 注入

## 交叉验证
- 与 `release-manager` 的构建产物步骤衔接。
