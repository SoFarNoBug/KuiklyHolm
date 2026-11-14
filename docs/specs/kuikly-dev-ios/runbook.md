# runbook · kuikly-dev-ios

> ADR 0009：iOS Runtime（`CAMetalLayer` + ObjC 主线程外泵 + C Session）。Lite `fragmentShader` 本波诚实失败。

## 结论

- 原生类：`iosApp/iosApp/KuiklyExpand/Views/KRHolmSurface.{h,m}` + `KRHolmMetalHostView`
- **类名 = viewName** `KRHolmSurface`
- Runtime：`holm-runtime` xcframework → `holm_runtime_session_*`；`onReady.backend=wgpu-m0*`
- Lite：深蓝 + `onError LITE_PENDING`（后单补 GLES 片元）
- Attr：`executor` / `fragmentShader` / `scene` / `playing` / `maxFps` / knobs + `onReady` / `onError` / `onPerf`

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
# Home →「示例1 / 双轨」(holm_runtime_m0)；默认 Runtime 隧道
```

Kotlin 侧若需先编 framework：

```bash
./gradlew :holmDemo:linkDebugFrameworkIosSimulatorArm64
```

（真机用 `iosArm64`；具体 target 以本机架构为准。）

## 验收勾选

- [ ] 模拟器 Runtime 隧道可见；`onReady backend=wgpu-m0*`
- [ ] 切 Lite → 深蓝 + `LITE_PENDING`（无假 GLES 动画）
- [ ] 切回 Runtime → 隧道恢复
- [ ] `maxFps` 30/60/0 可感知或采集 fps 合理
- [ ] 旋转 / resize / 二次进页不崩
- [ ] 真机 arm64 同左
- [ ]（负向）未跑 `build-ios.sh` → 链接失败；create/tick 失败 → `BLOCKED`

## 已知限制

- iOS Lite 宿主 GLSL 本波不做（ADR 0009 顺序：先 Runtime）
- xcframework **不**入库（`holm-runtime/dist/` gitignore）；本机脚本生成
- Windows CI **无法**跑本端
