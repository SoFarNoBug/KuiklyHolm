# changelog · business-custom-shader

## 2026-11-16（审查整改 · onShaderCompiled P2）

- [MODIFY] H5 Runtime Worker：`setCustomFs` 失败保留 `compileMs`（关 P2-h5-runtime-fail-compileMs-zero）
- [MODIFY] Ohos Lite：`!liteGl.ready` 仍 `FireShaderCompiled(ok=false)`（关 P2-ohos-lite-not-ready-silent）
- [MODIFY] 四端 Runtime 空片元回调改为 `ok=false`（关 P2-runtime-empty-ok-true）
- 本单无真机验证（H5 联调勾表）
- 轻量治理：规范自检见会话实现回报；gate 跳过（纯业务）

## 2026-11-16（onShaderCompiled 双耗时）

- [ADD] `HolmSurface` 事件 `onShaderCompiled`：`ok` / `wrapMs` / `compileMs` / `executor`；成败都报；失败先 `onError` 再本事件
- [ADD] 四端 Lite+Runtime 装载尝试结束回调（含拒绝零耗时、Surface 恢复重编）；Runtime `wrapMs=0`
- [ADD] Demo `holm_runtime_m0` 状态行展示最近一次耗时
- [ADD] ADR [0012](../../adr/0012-on-shader-compiled-timing.md)
- 本单无真机验证（编译门禁 + Demo 状态行自测）
- 轻量治理：规范自检见会话实现回报；gate 跳过（纯业务）

## 2026-11-13（采集改控制台）

- [MODIFY] Demo `holm_runtime_m0`：去掉页面 JSONL /「清空 JSON」；`println("[holm-perf] …")` 静默打控制台（H5→console）；仍 10s 自动停
- 本单无真机验证（H5 DevTools / AI 读 console）
- 轻量治理：规范自检见会话实现回报；gate 跳过（纯业务）

## 2026-11-13（采集 10s 自动停）

- [MODIFY] Demo `holm_runtime_m0`：「开始采集」默认采 10s（`LaunchedEffect`+`delay`）后自动停；可提前停；文案提示复制 JSONL 给 AI
- 本单无真机验证（H5 点采集等 10s 看停采即可）
- 轻量治理：规范自检见会话实现回报；gate 跳过（纯业务）

## 2026-11-13（审查整改 · P0/P1/P2）

- [MODIFY] H5 `HolmGlPumpSource`：`scene` 首次成功 rebuild 补 `ready` + `start`（关 P0-silent-fail）
- [MODIFY] Compose Attr：`fragmentShader`/`vertexShader` 先于 `executor`；JS 侧错方言热换跳过（关 P1-concurrency）
- [MODIFY] wrap 失败返回空串不回 frost；Demo `onPerf` 出帧清 sticky error
- 已知遗留：`ts` 仍为 sample 序号；Android Runtime 下一单；`P1-device-verify` 仍须 H5 勾表
- 本单无真机验证（整改后建议复审 + 勾 `device-check-list/business-custom-shader.md`）

## 2026-11-13（业务片元 + 双轨采集）

- [ADD] `HolmSurface` Attr `fragmentShader` / `vertexShader`；Compose 入参同步
- [ADD] `HolmShadertoyShell` / `HolmShaderResolve`（Lite：Shadertoy 正文 → GLES2/WebGL1）
- [MODIFY] Android / H5 Lite：自定义重链、`u_resolution`、失败保留上次程序 / 深蓝底 + `onError`
- [ADD] `holm-runtime` `setCustomFs` + H5 泵 / `HolmRuntimeNative` 接线；重建 wasm（`holm_runtime_bg.wasm` ≈ 3.85 MiB）
- [MODIFY] Demo `holm_runtime_m0`：custom 档、Lite/Runtime 切轨、霓虹隧道样本、JSONL 采集（含宽高）；Home 文案
- [ADD] ADR 0005；glossary / CONTEXT 术语
- 已知遗留：`ts` 字段本波为 sample 序号（非墙钟）；Android Runtime `.so` **下一单**
- 本单无真机验证（H5 硬刷 custom + 双轨采集自测）
- 轻量治理：规范自检见会话实现回报；gate 跳过（纯业务）
