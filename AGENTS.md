# kuikly-holm · AI 协作路由

## 项目概况

- 产品：**Holm**（**通用渲染底座**：shader-in / surface-out）
- 身份：**ADR 0007**；目标拆包：**ADR 0008**（`holm-core` / `holm-gl` / `holm-wgpu` + **Provider 必设**）
- 根工程：`KuiklyHolmUI`；库 `:KuiklyHolmCore`；Demo `:holmDemo`
- 命名：`HolmSurface` / `KRHolmSurface` / `holm_*`
- 形态：现状胖 Core + `holm-runtime`；**目标**三包可插拔（尚未物理拆）
- 节奏：稳固岛与 shader 契约 → 文档目标架构 → 另单拆包；Demo **主推隧道**，五席叙事废弃

## 接手阅读顺序

1. 本文件
2. [CONTEXT.md](CONTEXT.md)
3. [docs/adr/0007-holm-as-generic-render-substrate.md](docs/adr/0007-holm-as-generic-render-substrate.md)
4. [docs/adr/0008-holm-core-gl-wgpu-packages.md](docs/adr/0008-holm-core-gl-wgpu-packages.md)
5. [docs/product/00-context-and-decisions.md](docs/product/00-context-and-decisions.md)
6. Cursor 协作：[.cursor/agents-guide.md](.cursor/agents-guide.md)

## 硬规则（摘录）

1. 身份以 **00 + ADR 0007/0008** 为准；禁止写成「专为粒子/专为玻璃」。
2. 目标：业务自选 `holm-gl` / `holm-wgpu`；**必须设 Provider**；仅 core 必报错。
3. 全量 Standard/wgpu 产品化须有书面选型；失败路径诚实。
4. 禁止系统 Liquid Glass / WebView+Three 冒充底座完成。
5. 局部岛、少岛；列表默认禁实时重岛。
6. 改产品决策先改 `docs/product/00`（必要时 ADR）。

## Cursor 配置（本仓）

- Rules / Skills / Hooks：`.cursor/`
- 规范素材：`docs/skill-references/`
- 产品 SoT：`docs/product/`
- 编码前确认闸：`.cursor/rules/coding-confirm-gate.mdc` + `node .cursor/hooks/gate-unlock.mjs`
- Kuikly Compose DSL：`.cursor/rules/kuiklyComposeDSL.mdc`
