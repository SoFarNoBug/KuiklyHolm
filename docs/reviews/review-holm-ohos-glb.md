# Review · holm-ohos-glb

- 结论：打回
- 依据：ADR 0019；Plan Ohos Model DualTrack；changelog `docs/specs/kuikly-dev-ohos/changelog.md` + `docs/specs/holm-content-gltf/changelog.md`；`./ohos_build.sh` SUCCESS
- 日期：2026-11-19
- diff：`HolmOhosModelBridge`；`HolmModelPass` / `HolmWgpuOhosBridge` Model；`holm_xc_model_gl` + plugin 互斥泵；ETS contentId/手势；`holm-gltf` ohosArm64 + Draco；`settings.ohos` 双 ohosInclude 命名；kmp-wgpu Ohos ModelPass ABI；rawfile models

## P0

### P0-silent-fail 切资产 / 解析失败后仍 isReady=旧 mesh
- 证据：`KuiklyHolmCore/.../HolmOhosModelBridge.kt:197-206`（`catch` 只清 `_pendingSnapshot`，**未**清 `_packed` / `_ready`）；同文件 `243` `isReady = _ready && _packed != null`；`holm_xc_plugin.cpp:419-433`（`modelAttach` 后仅当 `isReady==0` 才 `FireError`，否则继续建 GLES/ModelPass 并 `FireReady`）
- 影响：Duck 已就绪后切失败资产 / Tokyo decode 抛错 → 仍报 ready、继续画旧模、**不**发 `MODEL_LOAD`；与 `holm_api` / ADR 0019 fail-loud 矛盾。首装即失败路径尚可（`_ready` 本为 false）
- 处置：必修 — `catch`（及同 gen）须 `_packed=null`、`_ready=false`；宿主侧失败不得 `FireReady`

## P1

### P1-main-thread-io Model attach 同步 parse（含 Draco）且持 HolmXcMutex
- 证据：`HolmOhosModelBridge.kt:187-196` 同步 `loadBytes`+`GltfDocument.parse`；`holm_xc_plugin.cpp:1228-1233` / `419-420` 在 NAPI/`StartModelIfNeeded` 锁内调用 `modelAttach`；对比 iOS `HolmIosModelBridge` `dispatch_async` 后台 parse
- 影响：Tokyo / 大 GLB 切资产可长时间卡住 UI 与 OnFrame；ADR 0019 写「异步 parse」未落地
- 处置：必修（可与 P0 同单）— Worker/后台 parse + gen 校验回填；锁内禁止重 decode

### P1-plan-soft-drift ADR「异步 parse」Ohos 未实现
- 证据：ADR 0019 Decision §5；Ohos Bridge 无 async；iOS 已异步
- 影响：宣称与实现软偏离（未到硬漂移：双轨 Present 代码路径存在）
- 处置：必修 — 对齐异步，或回写 ADR 明确 Ohos 同步并接受卡顿边界（仍建议修 `P1-main-thread-io`）

### P1-device-verify Ohos Model 双轨出画 / Tokyo / 灯光 / 切轨
- 证据：代码审无法证明真机/DevEco HAP 出画、orbit、Draco、wgpu 非静默 GLES
- 影响：放行条件；编冒烟已过不等于 Present 闭环
- 处置：必修（真机勾表）— 可执行档 [`device-check-list/holm-ohos-glb.md`](device-check-list/holm-ohos-glb.md)；**建议在修 P0 后再跑**，尤其 #4/#5/#10

## P2

### P2-nudge-gles-yaw-dup
- 证据：`holm_xc_plugin.cpp:1260-1293` GLES nudge 双分支同改 `modelGl.yaw/pitch`；冗长注释
- 影响：可维护性；行为暂一致
- 处置：建议 — 收成单路径

### P2-model-session-map-unsync
- 证据：`HolmOhosModelBridge.kt:44` `_sessions` 为普通 `mutableMapOf`；现依赖 C++ `HolmXcMutex` 串行调用
- 影响：若未来有旁路 KN 直调则竞态；当前壳路径可接受
- 处置：可选 — 文档约定或加锁

## 规范候选观察

- Model attach / parse 失败必须清 `_packed`+`_ready`（或等价），禁止 isReady 仍指旧 mesh 却换 contentId｜可抄码农 ohos/android 池
- Ohos Model parse 须异步（对齐 iOS），禁止在持岛锁的 NAPI/帧路径同步 Draco｜可抄 ohos 池

## 真机

缺口：GLES/wgpu 真画、orbit、切资产保视角、Tokyo/Draco、灯光、切 executor、失败 `MODEL_LOAD`、进出稳定 — 代码审不了。  
可执行档：[`device-check-list/holm-ohos-glb.md`](device-check-list/holm-ohos-glb.md)（编冒烟已记；真机步待勾）。

## changelog

已见码农 append（kuikly-dev-ohos + holm-content-gltf）。打回后修复须再 append；审核员不代写。

## 待补桶

无。
