# CONTEXT · Holm（kuikly-holm）

## 一句话

在 Kuikly 页面里用局部 GPU 岛做高级视觉特效（及可选简单 3D）；业务优先、效果优先；统一 Runtime 不是默认必上。

## SoT

| 主题 | 路径 |
| --- | --- |
| 决策 / 身份 / 重量档 | [`docs/product/00-context-and-decisions.md`](docs/product/00-context-and-decisions.md) |
| 架构 | [`docs/product/01-architecture.md`](docs/product/01-architecture.md) |
| P0 硬问题 | [`docs/product/07-p0-hard-problems.md`](docs/product/07-p0-hard-problems.md) |
| 阶段 | [`docs/product/phases/`](docs/product/phases/) |
| 术语 / 证据 | [`docs/product/references/`](docs/product/references/) |
| 仓库形态 ADR | [`docs/adr/0001-repo-shape-fat-core-shell.md`](docs/adr/0001-repo-shape-fat-core-shell.md) |
| Stage 0 ADR | [`docs/adr/0002-stage0-lite-android-h5.md`](docs/adr/0002-stage0-lite-android-h5.md) |
| Lite 五席 ADR | [`docs/adr/0003-lite-official-five-scenes.md`](docs/adr/0003-lite-official-five-scenes.md) |
| AI 工作流 | [`.cursor/agents-guide.md`](.cursor/agents-guide.md) |

## 工程壳

| 项 | 值 |
| --- | --- |
| 根工程 | `KuiklyHolmUI` |
| 库 | `:KuiklyHolmCore` · `com.jlj.kuiklybase.holm` |
| Demo | `:holmDemo` · `com.jlj.kuiklyholmui` |
| 端 | `androidApp` / `iosApp` / `h5App` / `ohosApp`（ohos 另册 settings） |
| 形态 | **胖 Core** 同构壳；独立 `holm-runtime` **后置** |
| Stage 0 | **Lite(A)**（2026-11-12） |

## 领域词（短表）

- **Holm / HolmSurface / KRHolmSurface**：产品与 Kuikly 组件 / viewName
- **Lite / Standard / Full**：重量档（见 00）；**当前选 Lite**
- **Stage 0**：选型门禁（已关闭 → Lite）
- **岛**：局部 GPU 表面，不替换主 UI 树
- **霜化尖刺**：同源 GLSL ES；策略 A 程序化输入；Android GLES2 + H5 WebGL1
- **官方五席**：`frost` / `glow` / `liquid_approx` / `gradient_bg` / `fullscreen_warp`（ADR 0003）
- **liquidBoost / liquidBend / warpAmount / scrollSpeed**：按 scene 解耦的 Lite Attr（废弃 `ior`/`warp`）

更全见 [`docs/product/references/glossary.md`](docs/product/references/glossary.md)。

## 当前状态

- 已落地：协作壳 + 产品文档 + **Stage 0 Lite 官方五席**（`holm_spike`）
- 编译门禁：`:androidApp:compileDebugKotlin`；JS：Core / holmDemo / h5App `compileKotlinJs`
- 下一步：iOS 补跑；策略 B / Runtime 后置；质量档与黄金图非本档必做
