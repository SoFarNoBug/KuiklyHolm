# dualtrack-lite-vs-standard · changelog

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
