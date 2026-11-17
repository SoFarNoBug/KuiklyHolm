# runbook · kuikly-dev-ios

> ADR 0009：iOS Runtime（`CAMetalLayer` + ObjC 外泵）+ Lite GLES；Present 经 kmp-wgpu（ADR 0008 对齐）。

## 结论

- 原生类：`KRHolmSurface` 容器 + `KRHolmMetalHostView` + `KRHolmGlesHostView`
- **类名 = viewName** `KRHolmSurface`
- Runtime Present：`HolmDemoHolmWgpuIosBridge` → `HolmFullscreenPass` → `IosWgpuPlatform`（cinterop）→ `KmpWgpuNative.xcframework`；`onReady.backend=wgpu-m0*`
- **壳禁止**直调 `kmp_wgpu_session_*`（ripreg 验收）
- Lite：GLES2 Shadertoy；`onReady.backend=gles2-ios`；首包未到不抢报错；**已 onReady 后**空片元 → `FRAGMENT_REQUIRED`
- Provider：`HolmIosBridge.configure` 显式 register；切轨 / start 经 `requireProviderExecutor:`
- Attr：`executor` / `fragmentShader` / `scene` / `playing` / `maxFps` / `renderScale` / knobs + `onReady` / `onError` / `onPerf`
- `renderScale`：默认 `1`；Lite 基线=点像素；Runtime 基线=`points×nativeScale`

## 前置

| 项 | 约定 |
|---|---|
| macOS + Xcode | 必填（模拟器或真机） |
| Rust | `rustup` + targets `aarch64-apple-ios` / `aarch64-apple-ios-sim`（本波无 Intel sim x86_64；工程已 `EXCLUDED_ARCHS`） |
| CocoaPods | `OpenKuiklyIOSRender` + `holmDemo` |
| JDK | 打 KMP framework 时用 17 |

## 起服（Mac）

```bash
# 1) Runtime 原生库（改 Rust 后必重跑；Xcode 不自动编）
cd kmp-wgpu-native
./build-ios.sh
# → dist/ios/KmpWgpuNative.xcframework

# 2) KMP framework（改 Kotlin/iosMain 后）
cd ..
./gradlew :holmDemo:linkPodDebugFrameworkIosSimulatorArm64
# 真机：:holmDemo:linkPodDebugFrameworkIosArm64

# 3) Pods + 打开工程
cd iosApp
pod install
open iosApp.xcworkspace
# 选 Simulator / 真机 → Run
# Home →「示例1 / 双轨」(holm_dualtrack)；默认 Runtime 隧道；可切 Lite
```

## 验收勾选

- [ ] 模拟器 Runtime 隧道可见；`onReady backend=wgpu-m0*`
- [ ] 切 Lite → 隧道；`onReady backend=gles2-ios`（**无** `LITE_PENDING`）
- [ ] 切回 Runtime → 隧道恢复；backend 回 wgpu
- [ ] renderer-gl / renderer-wgpu 下 `maxFps` 30/60/0 可感知或采集 fps 合理
- [ ] `renderScale` 1.0→0.5：重场景 fps 可升、画面可变糊；回 1.0 不崩
- [ ] 旋转 / resize / 二次进页不崩
- [ ] 真机 arm64 同左
- [ ]（负向）Lite **已就绪后**清空 `fragmentShader` → 深蓝 + `FRAGMENT_REQUIRED`（冷启首包 defer，勿与 Compose 晚到 attr 混淆）
- [ ]（负向）未跑 `build-ios.sh` / create 失败 → `BLOCKED`（勿 GLES 冒充 Runtime）
- [ ]（负向）未 register Provider → `BLOCKED`（文案含 HolmProvider）
- [ ] ObjC 源无 `kmp_wgpu_session_*` Present 直调

## 已知限制

- ObjC Shadertoy wrap 与 Kotlin `HolmShadertoyShell` 须同文；改壳双端同单
- xcframework **不**入库（`kmp-wgpu-native/dist/` gitignore）；本机脚本生成；App 仍直链 xcframework 供符号
- iOS headless Compute：`kmp_wgpu_headless_*` + `ComputeReadback.ios`（Metal）；Demo `kmp_wgpu_compute`
- Windows CI **无法**跑本端
