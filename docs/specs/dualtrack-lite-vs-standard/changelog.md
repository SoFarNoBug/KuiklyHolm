# dualtrack-lite-vs-standard · changelog

## 2026-11-14（鸿蒙 Runtime ADR 0010）

- [ADD] `holm_runtime_session_create_ohos` + `build-ohos.sh` → `libholm_runtime.so`（wgpu 30 / ohos-only）
- [ADD] `libholm_xc.so` XComponent OnFrame 外泵 + `KRHolmSurface.ets`；Lite=`LITE_PENDING`
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan 执行 ADR 0010｜待批 0
- 自测：`./build-ohos.sh` 出 so；`holm_xc_plugin.cpp` 对 SDK 头可编；真机隧道待勾

## 2026-11-14（关 iOS P2-objc-shadertoy-drift）

- [MODIFY] iOS Lite：Shadertoy wrap/vert 单源走 holmDemo Kotlin API
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改关 P2｜待批 0
- 自测：Lite 隧道出画

## 2026-11-14（修 iOS Lite ~1fps）

- [MODIFY] iOS Lite GLES 缓冲 1x（对齐不乘 DPR）；避免隧道堵主线程导致 fps=0
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改｜待批 0
- 自测：Lite fps>0；Runtime 无改

## 2026-11-14（iOS Lite GLES）

- [ADD] iOS `KRHolmGlesHostView` + `KRHolmSurface` Lite 轨：Shadertoy 隧道；`onReady.backend=gles2-ios`
- [MODIFY] 去 `LITE_PENDING`；Runtime↔Lite 互斥子 View；链 OpenGLES
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan 补 ADR 0009 后单 Lite｜待批 0
- 自测：Duo/真机 Runtime↔Lite 隧道；backend 不粘 wgpu

## 2026-11-14（关 iOS review P2 DisplayLink/resize）

- [MODIFY] iOS `KRHolmSurface`：DisplayLink weak proxy；像素未变跳过 resize
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改关 P2｜待批 0
- 自测：Runtime 通面；旋转不崩

## 2026-11-14（修 iOS Runtime→Lite 崩溃）

- [MODIFY] iOS：切轨先到 GLSL 时跳过 `set_custom_fs`；C ABI `catch_unwind`
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：热修切轨 abort｜待批 0
- 自测：Duo 模拟器 Runtime↔Lite

## 2026-11-14（iOS Runtime · ADR 0009）

- [ADD] `holm-runtime`：`ios_c_api` C Session（`holm_runtime_session_*`）+ iOS `wgpu`/`metal` 依赖 + `include/holm_runtime.h`
- [ADD] `build-ios.sh` → `dist/ios/HolmRuntime.xcframework`（device + sim；dist gitignore）
- [MODIFY] `KRHolmSurface`：容器 + `KRHolmMetalHostView`（CAMetalLayer）；主线程 DisplayLink 外泵；`maxFps`；Lite=`LITE_PENDING`
- [MODIFY] `iosApp.xcodeproj`：链 HolmRuntime.xcframework + Metal/QuartzCore
- [MODIFY] iOS runbook / CONTEXT 状态 / holm-runtime README
- 已知遗留：Lite GLES 片元后单｜不修理由：ADR 0009 顺序
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan 落地 iOS Runtime｜待批 0
- 真机验证表：交审核员（模拟器/真机隧道 + Lite 负向）
- 自测：`./holm-runtime/build-ios.sh` → `pod install` → Run → `holm_runtime_m0`

## 2026-11-14（审查 android-runtime-dualtrack 整改）

- [MODIFY] `KRHolmSurfaceView`：切 Lite 主动 `fireReady(gles2)`（防 backend 粘 wgpu）；`_runtimeSurfaceValid`→`AtomicBoolean`；急拆超时再投幂等 destroy；卸岛 `quitSafely` + 再挂重建线程；非 WGSL custom → `onError SHADER_CUSTOM`
- [MODIFY] Demo `HolmRuntimeM0Pager`：切 executor 清 `backendId`/`backendText`
- 已知遗留：`HolmRuntimeNative._presentable` 仍进程级｜不修理由：Demo 单岛；多岛另单
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：关审查 P1/P2 可改项｜待批 0
- 真机验证表：`docs/reviews/device-check-list/android-runtime-dualtrack.md`（审核员已出；本单装包供勾）
- 自测：`:androidApp:installDebug` → `holm_runtime_m0` 切轨看 backend；进出页 3 次

## 2026-11-14（Lite LITE_BODY 去 pow 对齐 Runtime）

- [MODIFY] Demo `NeonTunnelShaders.LITE_BODY`：去掉片元 `pow(col, vec3(0.4545))`，与 `RUNTIME_FS` 一致，缓解 Lite custom 发白
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：补齐上一单 Lite 遗留双伽马｜待批 0
- 本单无真机验证
- 自测：`:androidApp:installDebug` → `holm_runtime_m0` Lite custom，对比 Runtime 发白是否缓解

## 2026-11-14（真机画质：Lite highp + Runtime 非 sRGB）

- [MODIFY] `HolmShadertoyShell`：优先 `highp`（修 Lite custom 隧道花屏噪点）
- [MODIFY] `session_core`：Surface format **优先非 sRGB**（修 Android Runtime 全席发白发雾）
- [MODIFY] Demo `NeonTunnelShaders.RUNTIME_FS`：去掉片元内 `pow(0.4545)` 双伽马
- [MODIFY] `misuse` 错误 18/19；`kuikly-dev-android/runbook` Runtime / 镜像 / 画质坑
- 治理收尾：门禁 跳过｜三问 是/是/否｜小结级：misuse 18/19｜待批 0
- 真机验证表：`docs/reviews/device-check-list/android-runtime-dualtrack.md`（审核员产出；码农只登记）
- 自测：rebuild `.so` + `:androidApp:installDebug` → `holm_runtime_m0`

## 2026-11-13（升 AGP 8.13.2 + compileSdk 37）

- [MODIFY] 根 `build.gradle.kts`：AGP `8.3.2`→`8.13.2`；去掉强制 R8 覆盖（交还 AGP 自带）
- [MODIFY] Gradle Wrapper `8.5`→`8.13`；各模块 `compileSdk=37`；`android.suppressUnsupportedCompileSdk=37`
- [MODIFY] 本机 `sdk.dir`→`D:\Android_SDK`（`platforms/android-37` 联到 `android-37.0`）
- [MODIFY] Wrapper / Maven **仅**腾讯云：`gradle` + `maven-public` + `gradle-plugins`（去掉 google/central 直连）
- [MODIFY] `compileSdk { release(37){ minorApiLevel=0 } }` + `buildToolsVersion=36.0.0`；`android.builder.sdkDownload=false`
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：工具链对齐本机仅有 SDK 37｜待批 0
- 本单无真机验证
- 自测：`:androidApp:compileDebugKotlin` **BUILD SUCCESSFUL**（Gradle 8.13 + AGP 8.13.2）

## 2026-11-13（Android Runtime DualTrack：SurfaceView + 外泵 + arm64 .so）

- [MODIFY] `holm-runtime`：共享 `session_core`；`android_jni` 从 `Surface`→`ANativeWindow` 建 Session；`build-android.ps1` → `androidApp/.../jniLibs/arm64-v8a`
- [MODIFY] `HolmRuntimeNative`（既有）+ `KRHolmSurfaceView`：Runtime 换 `SurfaceView`；`HandlerThread`+`Choreographer` 外泵 `tick`；急拆；Lite 还原 GLES
- [MODIFY] Demo `holm_runtime_m0`：去掉「Android Runtime 下一单」文案
- [MODIFY] CONTEXT / glossary / ADR 0006 / `holm-runtime/README`
- 已知遗留：真机 frost/glow/liquid/custom + logcat `[holm-perf]` 未跑｜不修理由：本单编译门禁；真机表交审核员
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan 接线 Android Runtime｜待批 0
- 真机验证表：`docs/reviews/device-check-list/android-runtime-dualtrack.md`（审核员产出；码农只登记）

## 2026-11-13（审查 r3 整改：executor 对称拆泵 / Worker 失败回退 / 竞态）

- [MODIFY] H5：`runtime→lite` 走 `destroyPump()` 再 `startPump()`（修 P0 Worker 残留）
- [MODIFY] H5：Worker `CREATE_SESSION` 等异步失败 → 拆泵 + 主线程回退一次，再失败 BLOCKED+ready（修 P0 卡死）
- [MODIFY] H5：`_attached` + wasm `setInterval` 可取消；`createSession` 卸岛守卫（P1-concurrency）
- [MODIFY] `backendId`：WebGPU=`wgpu-m0`，GL 后端=`wgpu-m0-gl`（P1 能力诚实）
- [MODIFY] uncapped 改回 rAF（降 P2 busy-spin）；Worker `reportPerf` 门控；Demo ready 清 errorText
- 已知遗留：主线程预载 + Worker 再载 wasm（双载）未解｜不修理由：探针与泵生命周期拆分，改动面大
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：r3 打回整改｜待批 0
- 本单无真机验证（表仍挂 P1-device-verify）

## 2026-11-13（Runtime maxFps=0 对齐高刷：uncapped timer + AutoNoVsync）

- [MODIFY] Runtime 泵：`maxFps<=0` 也走 `setTimeout(0)` 紧调度（Worker 上纯 rAF+WebGPU 常吃不满 165Hz，Lite WebGL 可以）
- [MODIFY] Session present：`AutoNoVsync` 优先；`desired_maximum_frame_latency=1`
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：uncapped 对齐 Lite 高刷｜待批 0
- 本单无真机验证（H5 Runtime 点 0 对比 Lite onPerf）

## 2026-11-13（maxFps>60：timer 泵 + Mailbox/Immediate present）

- [MODIFY] Runtime/Lite H5 泵：`maxFps>60` 用 `setTimeout` 调度（破 rAF 屏刷封顶）；改档重启调度
- [MODIFY] `holm-runtime` Session：present 优先 `Mailbox`/`Immediate`，避免 Fifo 卡 tick
- [MODIFY] 主线程 Runtime 回退环同样 timer/rAF 分流
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：120 档可达提交率｜待批 0
- 本单无真机验证（H5 点 120 看 onPerf；60Hz 屏观感仍可能合帧）

## 2026-11-13（Runtime Demo 可调 maxFps）

- [MODIFY] `HolmRuntimeM0Pager`：`maxFps` 档位 0/30/60/120 传入 `HolmSurface`（泵侧本已支持；Worker/主线程限提交）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：Demo 旋钮｜待批 0
- 本单无真机验证（H5 点 30 看 onPerf≈30）

## 2026-11-13（Runtime Demo 接 onPerf 显示真实提交 FPS）

- [MODIFY] `HolmRuntimeM0Pager`：接 `onPerf`，状态行显示 `fps=N (Xms)`（与 Lite 同语义：约 1s 窗真实 tick 提交率）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：Demo 接线｜待批 0
- 本单无真机验证（H5 硬刷 `holm_runtime_m0` 看状态行）

## 2026-11-13（Runtime 性能对齐 Lite：Worker + 缓冲 + shader）

- [MODIFY] H5 Runtime：**优先 Worker + OffscreenCanvas** 泵帧（`HolmRuntimePumpSource`）；主线程回退保留
- [MODIFY] 缓冲尺寸与 Lite **同策**：CSS 像素，**不默认乘 DPR**（根因：同屏像素曾翻 4~9 倍）
- [MODIFY] 默认 **maxFps 仍 60**；perf 仅 `onPerf` 时约 1s 上报；`playing=false` 停 rAF / tick 跳过 GPU
- [MODIFY] WGSL：frost 抖动 UV 单采样（去 13-tap）；liquid 去 soft_blur，chroma 条件化
- [MODIFY] `HolmRuntimeSession.create` 支持 `HtmlCanvasElement | OffscreenCanvas`
- 包体：`holm_runtime_bg.wasm` ≈ **3.85 MiB**（4032600 B）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：Runtime≥Lite 性能硬约束｜待批 0
- 本单无真机验证（浏览器对比 Lite/Runtime CPU；Android 未开）

## 2026-11-13（H5 Runtime Lite3：帧循环 + frost/glow/liquid 观感近似）

- [MODIFY] `holm-runtime`：`HolmRuntimeSession`（create/tick/resize/destroy + setScene/setLiquid）；WGSL 三席 + `clear_tri` fallback
- [MODIFY] H5：`HolmRuntimeNative` Session API；`KRHolmSurfaceView` Runtime rAF 泵帧；Demo 旁路页三席 chip + liquid 旋钮
- [MODIFY] `index.html`：`createSession` → `HolmRuntimeSession.create`
- 包体：`holm_runtime_bg.wasm` ≈ **3.84 MiB**（4029243 B）
- 同源说明：与 Lite GLSL **非逐字**，观感近似；对照形态 C（对 Lite 五席页肉眼比）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：计划 M-H5-0..4｜待批 0
- 本单无真机验证（浏览器目视三席；Android 未开）

## 2026-11-13（M0 可见三角：清屏同色误判「没内容」）

- [MODIFY] `holm-runtime`：`m0_present` 清屏 + WGSL 琥珀黄三角（对比深蓝底）
- [MODIFY] H5：present 成功后去掉 canvas CSS `background`，避免与 GPU 同色混淆
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：M0 通面可见性｜待批 0
- 本单无真机验证（浏览器硬刷 Runtime 页目视三角）

## 2026-11-12（装 rustc 打通 H5 wasm M0）

- [MODIFY] `holm-runtime`：wasm-bindgen `holm_runtime_m0_present`（wgpu WebGPU|WebGL2 清屏）；`build-wasm.ps1`
- [ADD] `h5App/.../resources/holm-runtime/{holm_runtime.js,holm_runtime_bg.wasm}` + `index.html` 预载 `window.__holmRuntime`
- [MODIFY] H5 `HolmRuntimeNative`：探测 ready/hasWgpu + `presentM0`；`KRHolmSurfaceView` runtime 异步 present，失败仍 `wgpu-blocked`
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：工具链解锁 + DualTrack B 旁路通面｜待批 0（已达标 0）
- 本单无真机验证（浏览器目视清屏待你开 8080/8083）

## 2026-11-12（审查整改：onError 重放 + Runtime 假通面）


- [MODIFY] Android/H5：`onError` 缓存并在回调注册时重放（修 P1-concurrency）
- [MODIFY] Android：`playing` 在 `executor=runtime` 时不切 Lite CONTINUOUSLY（P2-playing）
- [MODIFY] `HolmRuntimeNative`：`isAvailable` = library ∧ `isM0Presentable`（本波 presentable=false，防空 .so 假通面）
- [MODIFY] iOS：`onError` 同样 pending 重放；文档改「frost 逐字 / liquid 骨架同源」
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：审查必修+可改 P2｜待批 0（已达标 0）
- 本单无真机验证（P1-device-verify 仍待表）

## 2026-11-12（DualTrack 尖刺落地）

- [ADD] Track A：iOS `KRHolmSurface.{h,m}`（GLES2 `frost`/`liquid_approx`）；pbxproj 替换 Placeholder；`docs/specs/kuikly-dev-ios/runbook.md` 起服
- [ADD] Track B：`holm-runtime/` Rust+wgpu crate 骨架；本机无 rustc → **BLOCKED**（不静默换栈）
- [ADD] Attr `executor`（`lite`|`runtime`，默认 `lite`）；Android/H5 旁路探测 `HolmRuntimeNative`
- [ADD] Demo `holm_runtime_m0` 第二页；Home 默认仍进 Lite 五席
- [ADD] 对比表填表 + ADR 0004 **Keep Lite**（O0 不改）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按 DualTrack 计划尖刺收口｜待批 0（已达标 0）
- 本单无真机验证（iOS 需 Mac；Runtime 需 rustc）
