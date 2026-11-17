# ADR 0012: onShaderCompiled · 片元装载双耗时回调

- Status: Accepted
- Date: 2026-11-16

## Context

业务需要知道每次喂片元（含热换与 Surface/上下文恢复重编）的装载耗时。现有 `onReady` 只报岛/后端就绪且热换不重发；`onPerf` 只报提交 FPS；`onError` 无耗时。审讯锁定独立事件、成败都带耗时，且包壳与编译分开计量。

## Decision

1. **事件**：`HolmSurface` / `KRHolmSurface` 新增 `onShaderCompiled`；不塞进 `onReady` / `onPerf`。
2. **载荷**：`ok` + `wrapMs` + `compileMs` + `executor`（`lite` / `runtime`）。
3. **计量**：
   - **包壳耗时**（`wrapMs`）：Lite 把宿主正文补成可编译源码的墙钟；Runtime **恒为 0**。
   - **编译耗时**（`compileMs`）：Lite 为 `compile`+`link`（或等价）；Runtime 为同步 `setCustomFs`/pipeline 创建墙钟（本波不拆 naga/pipeline ABI）。
   - **不含**首帧 draw / present。
4. **触发**：每次装载尝试结束都发——业务改 `fragmentShader`/`vertexShader`/`scene`、空片元/错方言拒绝、Surface/上下文恢复重编。同字符串 Attr 早退不发。
5. **失败**：先走现有 `onError`（若该路径本来会发），再发 `onShaderCompiled(ok=false)`；未跑到的步骤填 `0`。静默 skip 的方言路径不新造错误码，仍可只发本事件。
6. **空片元（Runtime）**：卸 custom / ClearTri 路径仍回调，**`ok=false`**（与 Lite 空串拒绝对齐；不表示 GPU 崩溃）。
7. **范围**：Android / H5 / iOS / 鸿蒙 × Lite + Runtime 一版对齐。
8. **非目标**：不改 `kmp-wgpu-native` 分阶段 ABI；不报 GPU 异步编译完成时间。

## Consequences

- 业务可稳定「每次改片元等到一次装载结果 + 双耗时」。
- Demo 双轨页状态行可展示最近一次耗时作验收夹具。
- 与 ADR 0005（片元契约）、0004（双轨）并存；错误码集合不变。
