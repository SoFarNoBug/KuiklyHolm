# holm-runtime · DualTrack B（Standard / Lite3 近似）

> **状态（2026-11-13）**：H5 wasm **Session 泵帧** + 三席 + **`setCustomFs` 业务 WGSL** 已通。  
> Android：**arm64-v8a** `libholm_runtime.so` + JNI Session（Surface → ANativeWindow）；Kotlin 外泵。  
> 尖刺默认：**Rust + wgpu**（禁止静默换非 wgpu 栈）。

## 范围

- H5：`HolmRuntimeSession` + `onReady.backend=wgpu-m0`（或 `wgpu-m0-gl`）
- Android：`HolmRuntimeNative` JNI + `KRHolmSurfaceView` `SurfaceView` 换栈（ADR 0006）
- 席位：前三席观感近似 Lite；`clear_tri` 作 debug fallback；`setCustomFs` 热换自定义片元
- **不做（本波）**：逐字 GLSL≡WGSL、真 Backdrop、gradient/warp、改 O0、x86_64 / 多 ABI、Rust 内泵、iOS Runtime

## H5 解锁

```powershell
cd holm-runtime
.\build-wasm.ps1
# → h5App/src/jsMain/resources/holm-runtime/
# 再编 h5App + holmDemo pack，起 8080/8083，进 Runtime 旁路页切三席
```

`wasm-bindgen-cli` 须与 crate 锁定的 `wasm-bindgen` **精确一致**（当前 0.2.130）。

## Android 解锁

```powershell
# 需要：ANDROID_NDK_HOME、rustup target aarch64-linux-android、cargo-ndk
cd holm-runtime
.\build-android.ps1
# → androidApp/src/main/jniLibs/arm64-v8a/libholm_runtime.so
```

改 Runtime Rust 后须先跑脚本；Gradle **不**自动编 Rust。缺 `.so` → Demo `onError(BLOCKED)`，不静默 GLES。

## 验收

- [x] 工具链 + wasm-bindgen
- [x] H5 Session 泵帧
- [x] H5 `setCustomFs` + ClearTri（官方五席 WGSL 已删）
- [x] Android arm64 `.so` + JNI Session API
- [ ] Android 真机隧道 custom + logcat `[holm-perf]`
- [ ] resize / destroy 专项压测
- [x] H5 wasm 包体 ≈ 3.84 MiB（写入 DualTrack changelog）
