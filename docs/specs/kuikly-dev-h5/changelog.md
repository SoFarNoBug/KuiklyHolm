# changelog · kuikly-dev-h5

## 2026-11-20（ADR 0020 · r3 全修 + 浏览器复验）

- [FIX] P1：H5 Model GLB `parse` 走 Blob Worker（绝对 URL 挂 Draco wasm）；失败/超时回退主线程 `GltfDocument.parse`
- [FIX] P2：同源 `solidColor` 热更改 `packed.color`（勿整模重载）；`fromBytes` 过 NVI `modelBytesB64`
- [FIX] P2：wgpu Model `maxFps in 1..59` 累加器减余量（勿清零偏慢）
- [NOTE] Chrome 复验：#7–9 wgpu≈60fps、切轨出画；#12 进出页稳定；#10 Safari 未测；#11 Demo 无错误资产 chip（缺文件 HTTP 404 + 代码 `MODEL_LOAD`）
- 轻量治理：路径=速速改｜sediment=否｜gate=已解锁｜真机表=device-check-list/holm-h5-glb.md｜审查回执行=r3 全开项
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：Worker/桥热更属既有 ADR 0020 执行面｜待批 0（已达标 0）

## 2026-11-20（ADR 0020 · Tokyo Draco + 灯光桥）

- [FIX] H5 Model wgpu 泵帧对齐 WebGL：默认每 rAF 出画（`maxFps≥60/0` 不再累加器半帧≈30）
- [FIX] 灯光热更保留拖转轨道角（勿用 bridge yaw/pitch 初值盖掉 `_modelYaw/_modelPitch`）
- [FIX] 切资产后 `onModelDocumentReady` 始终再发 `onReady`；同源 `_modelLoadedSource` 只热更灯（禁 Unlit 整模重载）
- [FIX] Demo 灯光按 state 重建 `HolmModelContent`（H5 NVI 须 bridge，勿只 `setLights`）
- [FIX] 依赖 `kmp-draco` js `toUint8Array`（原 `(intermediate value) is not a function`）→ Tokyo `KHR_draco`
- 轻量治理：路径=速速改｜sediment=否｜gate=已解锁｜真机表=device-check-list/holm-h5-glb.md｜根因=Draco U8 + 灯光未桥 + ready/重载

## 2026-11-19（ADR 0020 · H5 Model 双轨）

- [FIX] H5 双包 Registry 不可达：Compose 桥 `modelAssetPath`/`modelLights` 等；`KRHolmSurfaceView` 重建 `HolmModelContent` → Flamingo `onReady backend=webgl-model`
- 轻量治理：路径=速速改｜sediment=否｜gate=已解锁｜真机表=device-check-list/holm-h5-glb.md｜根因=nativevue2≠h5App Registry
- [FIX] `CONTENT_KIND_MODEL` 晚到拆已起 Lite；空 scene+空片元静默（不 `SHADER_WRAP`）；Model 过渡跳过 fragment/vertex `postScene`
- 轻量治理：路径=速速改｜sediment=否｜gate=已解锁｜真机表=device-check-list/holm-h5-glb.md｜审查回执行=SHADER_WRAP
- [FIX] r2 P0：Model 禁 Lite Worker/Offscreen；`resolveLite` 跳过防 `SHADER_WRAP`；Offscreen 检测换元重试；attach 失败也 ready
- 轻量治理：路径=速速改｜sediment=否｜gate=已解锁｜真机表=device-check-list/holm-h5-glb.md｜审查回执行=r2 P0-silent-fail
- [FIX] 审查 holm-h5-glb：P0 切轨一律 `replaceCanvasInPlace`；Draco `__kmpDraco.ready` + load gen 按 View；失败也 `onReady`；MVP Float32 复用；parse 让出一帧（仍主线程，Worker 另单）
- 轻量治理：路径=速速改｜sediment=否｜gate=已解锁｜真机表=device-check-list/holm-h5-glb.md｜审查回执行=P0/P1/P2
- [ADD] `HolmWebGlModelRenderer` / `HolmJsModelPresenter` / `HolmModelPass` js；`index.html` 挂 kmp-draco（独立 bindgen）+ `createModelSession`；静态 `models/*.glb`
- [MODIFY] `KRHolmSurfaceView` Model 分轨：gl 真画 / wgpu ModelPass；无 WebGPU → `BLOCKED`（禁止 WebGL 冒充）
- 轻量治理：路径=按计划行事｜sediment=否｜gate=已解锁｜真机表=device-check-list/holm-h5-glb.md（待审核员）

## 2026-11-17（ADR 0014 · 消灭 holm-runtime 命名）

- [MODIFY] native / C ABI / 产物 / Bridge·Demo 名对齐 `kmp-wgpu-native` / `kmp_wgpu_*` / `HolmWgpu*` / `holm_dualtrack`（详见 dualtrack changelog + ADR 0014）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan｜待批 0

## 2026-11-16（renderScale）

- [ADD] `PROP_RENDER_SCALE`：canvas backing = CSS×scale（Lite+Runtime）；`createSession(width,height)`
- 默认 `1`=CSS 基线（仍不乘 DPR）；本单无真机验证
- 轻量治理：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan｜待批 0

## 2026-11-16（onShaderCompiled）

- [ADD] Lite pump / Runtime `setCustomFs` 上报 `onShaderCompiled`（ADR 0012）
- 本单无真机验证
- 轻量治理：规范自检见会话实现回报；gate 跳过（纯业务）

## 2026-11-12（H5 双端 + runbook）

- 双端开发口：`8083`（`nativevue2.js`）+ `8080`（`jsBrowserDevelopmentRun`）；样页 `home` / `holm_spike`
- 新增 [runbook.md](./runbook.md)
- 工作流迁入 kuikly-holm；`project_id` = `kuikly-holm`；L3 = `holm_api`
