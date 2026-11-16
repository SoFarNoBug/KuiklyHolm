# runbook · kuikly-dev-ios

> ADR 0009：iOS Runtime（`CAMetalLayer` + C Session）+ Lite GLES（`CAEAGLLayer` 子 View + Shadertoy）。

## 结论

- 原生类：`KRHolmSurface` 容器 + `KRHolmMetalHostView` + `KRHolmGlesHostView`
- **类名 = viewName** `KRHolmSurface`
- Runtime：`holm-runtime` xcframework → `holm_runtime_session_*`；`onReady.backend=wgpu-m0*`
- Lite：GLES2 Shadertoy；`onReady.backend=gles2-ios`；空片元 → `FRAGMENT_REQUIRED`
- Attr：`executor` / `fragmentShader` / `scene` / `playing` / `maxFps` / `renderScale` / knobs + `onReady` / `onError` / `onPerf`
- `renderScale`：默认 `1`；Lite 基线=点像素（历史硬编码 1× 现可再降）；Runtime 基线=`points×nativeScale`

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
cd holm-runtime
./build-ios.sh
# → dist/ios/HolmRuntime.xcframework

# 2) Pods + 打开工程
cd ../iosApp
pod install
open iosApp.xcworkspace
# 选 Simulator / 真机 → Run
# Home →「示例1 / 双轨」(holm_runtime_m0)；默认 Runtime 隧道；可切 Lite
```

Kotlin 侧若需先编 framework：

```bash
./gradlew :holmDemo:linkDebugFrameworkIosSimulatorArm64
```

（真机用 `iosArm64`；具体 target 以本机架构为准。）

## 验收勾选

- [ ] 模拟器 Runtime 隧道可见；`onReady backend=wgpu-m0*`
- [ ] 切 Lite → 隧道；`onReady backend=gles2-ios`（**无** `LITE_PENDING`）
- [ ] 切回 Runtime → 隧道恢复；backend 回 wgpu
- [ ] Lite / Runtime 下 `maxFps` 30/60/0 可感知或采集 fps 合理
- [ ] `renderScale` 1.0→0.5：重场景 fps 可升、画面可变糊；回 1.0 不崩
- [ ] 旋转 / resize / 二次进页不崩
- [ ] 真机 arm64 同左
- [ ]（负向）Lite 空 `fragmentShader` → 深蓝 + `FRAGMENT_REQUIRED`
- [ ]（负向）未跑 `build-ios.sh` → 链接失败；create/tick 失败 → `BLOCKED`（勿 GLES 冒充 Runtime）

## 已知限制

- ObjC Shadertoy wrap 与 Kotlin `HolmShadertoyShell` 须同文；改壳双端同单
- xcframework **不**入库（`holm-runtime/dist/` gitignore）；本机脚本生成
- Windows CI **无法**跑本端
