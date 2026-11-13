# ADR 0006: Android Runtime · SurfaceView + Kotlin 外泵 Session

- Status: Accepted
- Date: 2026-11-13

## Context

H5 DualTrack Runtime（wasm Session 泵帧 + 三席 + `setCustomFs` + `[holm-perf]`）已通。Android 旁路此前恒 `BLOCKED`（无 `.so` / 无 Session）。审讯定稿：真机对照通面，失败不得静默回 GLES。

## Decision

1. **呈现面**：`executor=runtime` 时整岛换为 `SurfaceView`；切回 Lite 再挂 `GLSurfaceView`。
2. **泵模型**：Kotlin **渲染线程**（`HandlerThread` + `Choreographer`）拥有节拍，JNI `tick(dt)`；Rust **无内泵**。
3. **ABI**：本波只打 **`arm64-v8a`**（`holm-runtime/build-android.ps1` → `androidApp/src/main/jniLibs/arm64-v8a`）。
4. **生命周期**：`surfaceDestroyed` / 切 Lite / 卸岛 → **立刻** Session `destroy`（UI 线程等待渲染线程收尾后再返回）；新 Surface 再 `create`。
5. **呈现判定**：`isM0Presentable` = 库已载且至少一次 `create`+`tick` 成功；缺 `.so` / create 失败 → `onError(BLOCKED)` + 深蓝底，**禁止**用 GLES 冒充 wgpu。

## Consequences

- 改 Runtime 须先跑 `build-android.ps1`；Gradle 不强制编 Rust。
- 非 arm64 设备本波不验收 Runtime 通面（预期缺库 BLOCKED）。
- 与 H5 共享 `session_core` / WGSL 席位；自定义片元走非空 WGSL `fragmentShader` → `setCustomFs`。
