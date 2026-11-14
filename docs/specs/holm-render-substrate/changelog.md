# holm-render-substrate · changelog

## 2026-11-14（审查收口：重编 Runtime 产物 + 文档/iOS 死代码）

- [MODIFY] 重编并入仓 `libholm_runtime.so` / `holm_runtime_bg.wasm`（产物已无 `holm-frost`/`holm-glow`/`holm-liquid`）
- [MODIFY] android runbook 去掉 `holm_spike` 尖刺节；CONTEXT 状态对齐「五席已删」
- [MODIFY] `docs/product/00` / ADR 0008：五席源码改为「已删」
- [MODIFY] iOS `KRHolmSurface.m`：去掉死 vert/ unused GL 状态，仍清屏 + `FRAGMENT_REQUIRED`
- 关闭审查：`P1-plan-soft-drift` / `P1-doc-drift` / `P2-ios-dead-vert` / `P2-product-adr-stale`（`P1-device-verify` 仍待真机）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：收口审查项｜待批 0
- 本单无真机验证

## 2026-11-14（删除官方五席源码 · Demo 只留示例1）

- [DELETE] Core：`Frost/Glow/LiquidApprox/GradientBg/FullscreenWarpShaderSource` + `HolmSceneShaders`；根目录 `shaders/*` 五席文件；Demo `HolmSpikePager`
- [ADD] `HolmBuiltinShaders`（仅全屏 vert）；`HolmShaderResolve` 无宿主片元则抛错
- [MODIFY] Android/JS View 默认 `_scene=""`；空 fragment → error/清屏（不回 frost）
- [MODIFY] holm-runtime：删 FROST/GLOW/LIQUID WGSL 与三套 pipe；空 custom → ClearTri
- [MODIFY] iOS `KRHolmSurface`：去内嵌 frost/liquid；`FRAGMENT_REQUIRED` + 清屏
- [MODIFY] Demo：Home / `holm_runtime_m0` 只留「示例1」隧道 + 双轨；无五席 chip
- [MODIFY] CONTEXT / glossary / ADR 0003 文首 / runbook / file-map / 真机表主路径 → 隧道
- known: `holm_api` 全文未本单重写｜不修理由：计划非目标；changelog 记一行
- note: Runtime `.so`/wasm 已于同日审查收口条目重编入仓
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：删席沿用底座契约｜待批 0
- 本单无真机验证

## 2026-11-14（审讯收口：三包目标 + Provider + Demo 隧道优先）

- [ADD] ADR [0008](../../adr/0008-holm-core-gl-wgpu-packages.md)：`holm-core` / `holm-gl` / `holm-wgpu`；Provider 必设；可共存；岛可覆盖
- [MODIFY] `00` / CONTEXT / AGENTS / glossary / product README；ADR 0003 superseded 指针加强
- [MODIFY] Demo `HomePager`：主推隧道双轨；五席入口标废弃
- [MODIFY] Demo `HolmRuntimeM0Pager`：默认隧道；五席 chip 标废弃
- 后单：物理拆 Gradle、删五席源码、`holm_api` 规范
- 治理收尾：门禁 跳过｜三问 是/否/否｜理由：审讯锁定目标架构｜待批 0
- 本单无真机验证

## 2026-11-14（产品身份推翻 → 通用渲染底座）

- [ADD] ADR [0007](../../adr/0007-holm-as-generic-render-substrate.md)：Holm = shader-in / surface-out；不绑定单一业务
- [MODIFY] `docs/product/00-context-and-decisions.md`：重写身份 / Goals / D12；五席降为样本；Stage 0 标为历史产品结论已取代
- [MODIFY] `CONTEXT.md` / `AGENTS.md` / `docs/product/README.md` / `01-architecture.md` 文首 / `glossary.md`
- [MODIFY] ADR 0002 / 0003 / 0004 文首 `Superseded in part by 0007`；`docs/adr/README.md` 登记 0007
- 本单**仅文档**；未改 Core / Runtime / ParticleEngine 源码
- 治理收尾：门禁 跳过｜三问 是/否/是｜小结：身份与旧 D12 特效句冲突已由 0007/00 覆盖｜待批 0
- 本单无真机验证
