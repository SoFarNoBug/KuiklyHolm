# kuikly-holm · AI 协作路由

## 项目概况

- 产品：**Holm**（**通用渲染底座**：shader-in / surface-out）
- 身份：**ADR 0007**；目标拆包：**ADR 0008**（`holm-core` / `holm-gl` / `holm-wgpu` + **Provider 必设**）
- 下沉：**ADR 0013**（`kmp-wgpu` 原语层；`holm-wgpu` 为其上薄适配；与 Kuikly 无关）
- 根工程：`KuiklyHolmUI`；库 `:KuiklyHolmCore`；Demo `:holmDemo`
- 命名：`HolmSurface` / `KRHolmSurface` / `holm_*`；原语层 `kmp-wgpu` / `kmp-wgpu-surface`
- 形态：胖 Core + `holm-runtime`（=kmp_wgpu_native）；已有 `:kmp-wgpu` / `:kmp-wgpu-surface`（Android+H5）；Holm 三包尚未物理拆
- 节奏：岛契约稳 → ADR 0013 v1 抽芯（已）→ v1.1 Compute → 真拆三包；Demo **主推隧道**；五席源码已删

## 接手阅读顺序

1. 本文件
2. [CONTEXT.md](CONTEXT.md)
3. [docs/adr/0007-holm-as-generic-render-substrate.md](docs/adr/0007-holm-as-generic-render-substrate.md)
4. [docs/adr/0008-holm-core-gl-wgpu-packages.md](docs/adr/0008-holm-core-gl-wgpu-packages.md)
5. [docs/adr/0013-kmp-wgpu-primitives-under-holm.md](docs/adr/0013-kmp-wgpu-primitives-under-holm.md)
6. [docs/product/00-context-and-decisions.md](docs/product/00-context-and-decisions.md)
7. Cursor 协作：[.cursor/agents-guide.md](.cursor/agents-guide.md)

## 硬规则（摘录）

1. 身份以 **00 + ADR 0007/0008/0013** 为准；禁止写成「专为粒子/专为玻璃」。
2. 目标：业务自选 `holm-gl` / `holm-wgpu`；**必须设 Provider**；仅 core 必报错。
3. 通用 GPU 算力 / 图像加速走 **`kmp-wgpu`**，不要把 Holm 扩成算力 SDK。
4. 全量 Standard/wgpu 产品化须有书面选型；失败路径诚实。
5. 禁止系统 Liquid Glass / WebView+Three 冒充底座完成。
6. 局部岛、少岛；列表默认禁实时重岛。
7. 改产品决策先改 `docs/product/00`（必要时 ADR）。

## Cursor 配置（本仓）

- Rules / Skills / Hooks：`.cursor/`
- 规范素材：`docs/skill-references/`
- 产品 SoT：`docs/product/`
- 编码前确认闸：`.cursor/rules/coding-confirm-gate.mdc` + `node .cursor/hooks/gate-unlock.mjs`
- Kuikly Compose DSL：`.cursor/rules/kuiklyComposeDSL.mdc`
