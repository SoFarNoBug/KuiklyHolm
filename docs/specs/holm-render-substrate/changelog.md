# holm-render-substrate · changelog

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
