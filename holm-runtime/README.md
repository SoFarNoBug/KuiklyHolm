# holm-runtime · DualTrack B（Standard / Lite3 近似）

> **状态（2026-11-14）**：H5 wasm + Android JNI Session 已通；iOS C Session + xcframework 见 ADR 0009；鸿蒙 C Session + 双 so 见 ADR 0010。  
> 尖刺默认：**Rust + wgpu**（禁止静默换非 wgpu 栈）。

## 范围

- H5：`HolmRuntimeSession` + `onReady.backend=wgpu-m0`（或 `wgpu-m0-gl`）
- Android：`HolmRuntimeNative` JNI + `KRHolmSurfaceView` `SurfaceView` 换栈（ADR 0006）
- iOS：`holm_runtime_session_*` C ABI + `CAMetalLayer`；iosApp ObjC 主线程外泵（ADR 0009）
- 鸿蒙：`holm_runtime_session_create_ohos` + `libholm_runtime.so`；`libholm_xc.so` XComponent OnFrame 外泵（ADR 0010）；Lite 本波 `LITE_PENDING`
- 席位：`clear_tri` fallback；`setCustomFs` 热换自定义片元（官方五席 WGSL 已删）
- **不做（本波）**：逐字 GLSL≡WGSL、真 Backdrop、改 O0、Rust 内泵、Android 多 ABI、把 Android JNI 收拢到 C Session、鸿蒙 Lite GLES

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

## iOS 解锁

```bash
# 需要：rustup（aarch64-apple-ios + aarch64-apple-ios-sim）、Xcode
cd holm-runtime
./build-ios.sh
# → dist/ios/HolmRuntime.xcframework + Headers/holm_runtime.h
# iosApp 直链该 xcframework；改 Rust 后须重跑本脚本（Xcode 不自动编 Rust）
```

缺 xcframework → 工程链接失败；create/tick 失败 → Demo `onError(BLOCKED)`，禁止 GLES 冒充。

## 鸿蒙解锁

```bash
# 需要：rustup target aarch64-unknown-linux-ohos、OpenHarmony native SDK
# 可选环境变量：OHOS_SDK_NATIVE / OHOS_NDK_HOME / DEVECO_SDK_HOME
cd holm-runtime
./build-ohos.sh
# → ohosApp/entry/libs/arm64-v8a/libholm_runtime.so
# ohosApp CMake 另编 libholm_xc.so（XComponent）；改 Rust 后须重跑本脚本（Hvigor 不编 Rust）
```

缺 `.so` / create 失败 → Demo `onError(BLOCKED)`；`executor=lite` → `LITE_PENDING`。详见 `docs/specs/kuikly-dev-ohos/runbook.md`。

## 验收

- [x] 工具链 + wasm-bindgen
- [x] H5 Session 泵帧
- [x] H5 `setCustomFs` + ClearTri（官方五席 WGSL 已删）
- [x] Android arm64 `.so` + JNI Session API
- [ ] Android 真机隧道 custom + logcat `[holm-perf]`
- [x] iOS device+sim xcframework + ObjC 外泵接线（ADR 0009；真机隧道观感待勾）
- [x] 鸿蒙 aarch64 `libholm_runtime.so` + `libholm_xc` / `KRHolmSurface` 接线（ADR 0010；真机隧道待勾）
- [ ] resize / destroy 专项压测
- [x] H5 wasm 包体 ≈ 3.84 MiB（写入 DualTrack changelog）
