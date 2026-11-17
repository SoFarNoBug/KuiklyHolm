# 06 · 总路线图（Kuikly 业务优先）

## 1. 顺序

```text
Stage 0 → 结论 Lite(A) 或 Standard(B) 或终止
  ├─ Lite：先交 1～2 个 Kuikly 可用特效 → 扩官方特效 →（可选）再评估 Standard
  ├─ Standard：仍建议先有可用特效，再加深 Runtime；M0 通面 → M1 → M2 → M3
  └─ 终止：归档
```

**效果优先于引擎**：禁止长期只有三角清屏、业务零特效。

## 2. 仓库（多端同构 · 胖 Core）

根工程 **`KuiklyHolmUI`**（磁盘目录可仍为 `kuikly-holm`）。**不**要求先建旁路 `kmp-wgpu-native/` 目录；独立 Runtime 模块（若需要）**后置**于 Stage 0 / 业务特效之后。

```text
kuikly-holm/
  KuiklyHolmCore/       # 胖 Core：API / 声明 / Lite 挂载点将来进此模块（当前可几乎空）
  holmDemo/             # Demo 共享页
  androidApp/ iosApp/ h5App/ ohosApp/
  buildSrc/ gradle/
  docs/                 # 产品 SoT + AI 工作流
  .cursor/
```

| 角色 | 模块 / 包 |
| --- | --- |
| 根工程 | `KuiklyHolmUI` |
| 库 | `:KuiklyHolmCore` · `com.jlj.kuiklybase.holm` |
| Demo | `:holmDemo` · `com.jlj.kuiklyholmui` |
| 四端壳 | `androidApp` + `iosApp` + `h5App` + ohos（`settings.ohos.gradle.kts`）；mini 暂不 include |

不优先拆 `holm-uikit` / `holm-compose-android` 独立开源仓（D12）。`shaders/`、Lite 后端、独立 `:kmp-wgpu-native` 等为**后续**可选落点，非本壳前置。

## 3. 风险

| ID | 风险 | 缓解 |
| --- | --- | --- |
| R0 | 跳过 Stage 0 | D0 |
| R-id | 做成通用开源拖死业务 | D12 |
| R-rt | 强制 wgpu | D2：Lite 可不扛 |
| R3 | Backdrop | 07 推荐默认 |
| R5 | List 滥用 | D6 + 静态冻结 |
| R9 | 整页进岛 | 00 §4.5 |
| R10 | 只做基建无特效 | §1 效果优先 |

## 4. 禁止假完成

跳过 Stage 0；系统玻璃/WebView 冒充；改阈值过关；宣称多框架已支持；只有 Runtime 无业务特效。

## 5. Agent 提示词

```text
读 D:\code_space\kuikly-holm\docs\product\README.md、00（D12 Kuikly优先）、07、Stage0。
未选 Standard 禁止全量 M0–M3 Runtime。
锚点是 Shader 同源；命名 HolmSurface/KRHolmSurface 不改。
仓库是胖 Core 同构壳（KuiklyHolmCore + holmDemo），勿强制先建旁路 kmp-wgpu-native。
优先交付 Kuikly 可用特效。
```

---

**上一篇**：[05-consistency-qa-perf.md](./05-consistency-qa-perf.md)
