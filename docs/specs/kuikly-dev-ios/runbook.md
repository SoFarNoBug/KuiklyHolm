# runbook · kuikly-dev-ios

> DualTrack A：Lite iOS `KRHolmSurface`（OpenGLES2）。本机若无 macOS / Xcode，无法在此跑模拟器；代码与工程已落盘，验收在 Mac 上勾。

## 结论

- 原生类：`iosApp/iosApp/KuiklyExpand/Views/KRHolmSurface.{h,m}`  
- **类名 = viewName** `KRHolmSurface`（Kuikly iOS 约定）  
- 本波 scene：`frost` **逐字同源**；`liquid_approx` **骨架同源**（端侧允许精简，非强制逐字同步 common）  
- Attr：`scene` / `playing` / `liquidBoost` / `liquidBend` + `onReady` / `onError`

## 前置

| 项 | 约定 |
|---|---|
| macOS + Xcode | 必填（模拟器或真机） |
| CocoaPods | 工程既有 `Pods` / OpenKuiklyIOSRender |
| JDK | 打 KMP 框架时用 17 |

## 起服（Mac）

```bash
cd iosApp
pod install   # 若尚未
open iosApp.xcworkspace
# 选 Simulator → Run
# Demo 打开 page_name=holm_spike，切霜化/液态
```

Kotlin 侧若需先编 framework：

```bash
./gradlew :holmDemo:linkDebugFrameworkIosSimulatorArm64
```

（具体 target 以本机架构为准。）

## 验收勾选

- [ ] 模拟器可见 frost  
- [ ] 切换 liquid_approx 画面变化  
- [ ] onReady backend=`gles2-ios`  
- [ ] 旋转/resize 不崩  
- [ ] 二次进入页不崩  

## 已知限制

- glow / gradient_bg / fullscreen_warp **本波不强制** iOS  
- shader 字符串在 `.m` 内嵌；`frost` 改 common 后须同步 ObjC；`liquid_approx` 允许端侧精简，重大数学变更再对齐  
- Windows CI **无法**跑本端；对比表「本机验证」填 Mac 结果或 PARTIAL
