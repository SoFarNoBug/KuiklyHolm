# Review · ohos-lite-adr0011

- 结论：有条件通过
- 依据：Plan「鸿蒙 Lite GLES（ADR 0011）」；ADR 0011；changelog（ohos + dualtrack 2026-11-15）
- 日期：2026-11-15
- 范围：`libholm_xc` 同 XComponent EGL/GLES Lite；`gles2-ohos`；去 `LITE_PENDING`。不含 Android/iOS、不含物理拆包、不含 Lite 降采样

## 对照上轮

| 上轮（ADR 0010 / Runtime） | 本轮 |
|---|---|
| Lite=`LITE_PENDING` | 已关：EGL 出画路径 + `gles2-ohos`（真机未勾） |
| Runtime 接线已落 | 保留；切轨对称 DestroyLite ↔ DestroyRuntimeSession |
| — | 新开：真机双轨 / wrap 双源 / README 漂移 |

## P0

无。

## P1

### P1-device-verify（未关）Lite 通面与 DualTrack 切轨
- 证据：Plan 验收含 Lite 隧道 / `gles2-ohos` / Runtime↔Lite 互切 / 空 FS / WGSL 负向 / 离页急拆；本机无鸿蒙真机闭环
- 影响：放行条件未闭环；同窗 wgpu↔EGL 占窗是否干净须真机证
- 处置：必修 → 可执行档 [`device-check-list/ohos-lite-adr0011.md`](device-check-list/ohos-lite-adr0011.md)
- 代码审不了：真机 Lite 隧道是否出画；互切是否崩/黑屏；backend 是否粘 `wgpu-m0`；`eglCreateWindowSurface` 是否需 NativeWindow format；空片元 / WGSL 负向；进出页泄漏

### P1-doc-drift（未关）holm-runtime README 仍写 Lite=`LITE_PENDING`
- 证据：`holm-runtime/README.md` 仍称鸿蒙 Lite 本波 `LITE_PENDING`；与 ADR 0011 / runbook / 实现不符
- 影响：接手读 README 会以为 Lite 未做
- 处置：必修 — 码农改 README 对齐 0011（审核员不改业务/changelog）

## P2

### P2-cpp-shadertoy-drift
- 证据：`holm_xc_shadertoy.h` 内嵌 wrap/vert，与 `HolmShadertoyShell.kt` / `HolmBuiltinShaders.kt` 双文；changelog 已记同步义务
- 影响：改壳不同步则 Lite 编译/观感漂移（对齐 iOS 同类 P2）
- 处置：建议 — 改 SoT 须同单同步 C++

### P2-egl-terminate-default-display
- 证据：`HolmXcLiteGlDestroy` 对 `EGL_DEFAULT_DISPLAY` 调 `eglTerminate`
- 影响：若进程内另有 EGL 用户，Terminate 可能误伤；本仓单岛 Demo 风险低
- 处置：可选 — 后单改为仅 destroy context/surface、refcount 后再 Terminate

### P2-attr-order-transient-dialect
- 证据：`setExecutor`→Lite 时若 `fragmentShader` 仍为 WGSL，会先 `SHADER_DIALECT`；Demo 随后会换成 `LITE_BODY`（`HolmRuntimeM0Pager` 按 executor 选串）
- 影响：切轨瞬间可能闪深蓝+error；非静默无画
- 处置：可选 — 与 iOS「先 fragment 再 executor」同族；真机观察即可

## 规范候选观察

1. 同窗 DualTrack（wgpu Session ↔ EGL window surface）切轨后须真机证占窗释放；文档/README 不得残留 `LITE_PENDING` 史实当现行。
2. C++ 内嵌 Shadertoy wrap 与 Kotlin SoT 双源时，changelog 须强制同步义务（本单已留痕）。

（已入本 skill `_backlog`）

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/ohos-lite-adr0011.md`](device-check-list/ohos-lite-adr0011.md)。

## changelog

请确认已 append：
- `docs/specs/kuikly-dev-ohos/changelog.md`（2026-11-15 ADR 0011）
- `docs/specs/dualtrack-lite-vs-standard/changelog.md`（同日）  
（审核员不代写。）

## 待补桶

无。
