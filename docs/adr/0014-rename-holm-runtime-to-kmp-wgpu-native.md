# ADR 0014: 物理消灭 holm-runtime 命名（→ kmp-wgpu-native / kmp_wgpu_*）

- Status: Accepted
- Date: 2026-11-17

## Context

ADR 0013 已锁定长期身份为 `kmp-wgpu` / `kmp_wgpu_native`，但磁盘目录、Cargo lib、产物文件名、C ABI、H5 glue、Kotlin `HolmRuntime*` 与 Demo 路由仍沿用 `holm-runtime` / `holm_runtime` / `HolmRuntime*`。命名污染使分层读成「Holm 底下还有 Holm Runtime」。

## Decision

1. **目录 / crate / 产物**：`holm-runtime/` → `kmp-wgpu-native/`；Cargo/lib `kmp_wgpu_native`；`libkmp_wgpu_native.so` / `kmp_wgpu_native*.wasm` / `KmpWgpuNative.xcframework`。
2. **C ABI**：`holm_runtime_*` → `kmp_wgpu_*`（header `kmp_wgpu_native.h`）。**故意 ABI break**；**不**保留双符号 / weak alias / compat shim。
3. **H5**：资源目录 `kmp-wgpu-native/`；wasm 类型 `KmpWgpuSession`；全局 `__kmpWgpu`。
4. **Holm 适配 Kotlin**：`HolmRuntimeNative` / `HolmRuntimePumpSource` / `HolmRuntime*Bridge` → `HolmWgpuNative` / `HolmWgpuPumpSource` / `HolmWgpu*Bridge`（仍属 Holm 层，仅去掉 Runtime 产品字样）。
5. **Demo**：`@Page("holm_runtime_m0")` → `@Page("holm_dualtrack")`；类 `HolmDualTrackPager`。
6. **历史文档**：changelog / 旧审查报告不考古改写；文中 `holm-runtime` 一律理解为本 crate 的旧名。

## Consequences

- 旧 so / wasm / xcframework / ObjC bridge 名全部失效，须四端重编。
- 旧文中的 `holm-runtime` = 今日 `kmp-wgpu-native`（`kmp_wgpu_native`）。
- Holm 产品 ABI（`HolmSurface` / `KRHolmSurface` / `holm_ohos_bridge_*` 等）不变。

## Relates

- ADR 0013（kmp-wgpu 原语层）
- ADR 0008（Holm 三包；叠在 kmp-wgpu 之上）
