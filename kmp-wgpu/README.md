# kmp-wgpu（ADR 0013 · v1）

与 Holm / Kuikly **无关**的跨端 wgpu **原语库**（Kotlin 脸）。

## 方案 B（已锁定）

- **类型**：近 wgpu 1:1 概念（`Device` / `Queue` / `ShaderModule` / …）
- **桥接**：粗粒度能力块（一次 `sessionTick` = encode + submit + present）；**禁止**逐 `draw` JNI
- **Present**：可选，见 [`../kmp-wgpu-surface`](../kmp-wgpu-surface)

## v1 支持

| API | 状态 |
| --- | --- |
| `WgpuRuntime` / `Instance` / `Device` / `Queue` | 有（Device=Session 句柄） |
| `CommandEncoder.recordFullscreenTick` | 壳；提交走 `sessionTick` |
| Buffer / Texture / BindGroup / Pipeline 类型 | 壳占位 |
| Compute / 回读 | **v1.1** |
| 逐条 CommandEncoder FFI | **不做**（方案 C，另 ADR） |

## 非本模块

`setCustomFs` / `setScene` / `liquid*` / `playing` 属 **Holm 适配**（`HolmFullscreenPass` / `HolmRuntimeNative`），不在 `kmp-wgpu` 包下。

## Native

Rust crate 目录仍为 `holm-runtime/`，长期名 **`kmp_wgpu_native`**；产物文件名暂 `libholm_runtime.so` / `holm_runtime*.wasm` 兼容。
