# kuikly-holm · AI 协作路由

## 项目概况

- 产品：**Holm**（Kuikly 局部 GPU 特效增强）
- 身份：**Kuikly 业务优先**（D12）；非第一期通用多框架引擎
- 根工程：`KuiklyHolmUI`；库 `:KuiklyHolmCore`；Demo `:holmDemo`
- 命名：`HolmSurface` / `KRHolmSurface` / `holm_*`
- 形态：**胖 Core** 同构壳（独立 Runtime 后置）；见 [ADR 0001](docs/adr/0001-repo-shape-fat-core-shell.md)
- 节奏：先 **Stage 0**（Lite vs Standard），再特效交付 / Runtime

## 接手阅读顺序

1. 本文件
2. [CONTEXT.md](CONTEXT.md)
3. [docs/product/README.md](docs/product/README.md) → [00-context-and-decisions.md](docs/product/00-context-and-decisions.md)
4. [docs/product/07-p0-hard-problems.md](docs/product/07-p0-hard-problems.md)
5. [docs/product/phases/Stage0.md](docs/product/phases/Stage0.md)
6. Cursor 协作：[.cursor/agents-guide.md](.cursor/agents-guide.md)

## 硬规则（摘录）

1. 未过 Stage 0 禁止全量 Standard Runtime（M0–M3）。
2. 锚点是同源 Shader/Pass；wgpu Runtime 可选（Lite 可不扛）。
3. 禁止系统 Liquid Glass / WebView+Three 冒充完成。
4. 局部岛、少岛；列表默认禁实时重特效；Backdrop 见 product/07。
5. 改产品决策先改 `docs/product/00`（必要时 ADR）。

## Cursor 配置（本仓）

- Rules / Skills / Hooks：`.cursor/`
- 规范素材：`docs/skill-references/`
- 产品 SoT：`docs/product/`
- 编码前确认闸：`.cursor/rules/coding-confirm-gate.mdc` + `node .cursor/hooks/gate-unlock.mjs`
- Kuikly Compose DSL：`.cursor/rules/kuiklyComposeDSL.mdc`
