# 待沉淀池（审查 skill 自有）

> 候选只进**本文件**。不得自动写入码农的 `_backlog`（ADR 0036）。
> `project.yaml` 的 `peer_pools` 仅提示可人工抄录的路径。
> 候选只保留与本仓无关业务细节的通用判据；晋升或作废走人工评审。

**条目格式（候选）**：`- [ ] {规则一句话} ｜{日期} ｜自评：{已达标 / 未达标（缺什么）}`

## 候选

- [ ] H5 双包（业务 nativevue2 ≠ 宿主 h5App）时 HolmContentRegistry 不可共享；Model 须 NVI bridge 载荷（对齐 Shader fragment 桥），否则静默无 Present｜2026-11-20｜自评：已达标（holm-h5-glb-r3 代码；仍须防 fromBytes/solid 漏桥）
- [ ] H5 Model Present 禁止先挂 Lite Worker/Offscreen；与 Model WebGL 争用同一 canvas 即主路径失效｜2026-11-19｜自评：已达标（holm-h5-glb-r3 Chrome；原 r2 P0）
- [ ] H5 Model wgpu 泵帧勿对默认 maxFps=60 用 rAF 累加器（易半帧）；应对齐 WebGL 每 rAF｜2026-11-20｜自评：已达标（holm-h5-glb-r3）
- [ ] H5/Web 上 WebGL 与 WebGPU 不得复用同一 canvas 切换 Present；切轨须换元并重绑手势｜2026-11-19｜自评：已达标（代码 r1→r2；仍须配合 Offscreen 禁争用）
- [ ] H5 Model Draco/wasm 须有 ready 门闸（对齐 `__kmpWgpu.ready`），禁止竞态 MODEL_LOAD｜2026-11-19｜自评：已达标（holm-h5-glb-r2；冒烟 ready=true）
- [ ] Ohos GLES Model mesh 上传只消费 pending snapshot，禁止每帧 currentPacked/glBufferData 整模重灌｜2026-11-19｜自评：已达标（holm-ohos-glb-r3；Tokyo 帧预算）
- [ ] Model attach/parse 失败须清 packed+ready（或等价），禁止 isReady 仍指旧 mesh 却已换 contentId｜2026-11-19｜自评：未达标（holm-ohos-glb P0-silent-fail）
- [ ] Ohos Model parse 须异步（对齐 iOS），禁止持岛锁的 NAPI/帧路径同步 Draco｜2026-11-19｜自评：未达标（holm-ohos-glb P1-main-thread-io）
- [ ] iOS Model Present 状态按岛实例分表，避免全局 `object` Bridge 多岛互踩｜2026-11-19｜自评：未达标（holm-ios-glb P1-concurrency）
- [ ] 本地 xcframework 旁路（`kmp-*-native` symlink）须在 guide/README 写明，干净 clone 可复现链库｜2026-11-19｜自评：未达标（holm-ios-glb P2）
- [ ] Content Registry 过 NVI：View 销毁/卸岛必须 unregister 当前 contentId，避免 map 泄漏｜2026-11-18｜自评：未达标（holm-content-gltf P2）
- [ ] Gradle 物理拆后若 umbrella 自动 `register`+`setDefault`，ADR/产品文须写明「兼容例外」，否则与「禁止仅靠 classpath 静默默选」冲突｜2026-11-16｜自评：未达标（holm-three-package-split P1-doc-drift）
- [ ] 跨 FFI 包装 `Result`/throw 时禁止再用 sticky `lastError*` 二次否决成功路径；`drain` 若不清 last，宿主不得把 last 当帧成败｜2026-11-16｜自评：未达标（kmp-wgpu-extract-v1 P0）
- [ ] DualTrack 性能对比须标注 `renderScale` 与端基线（物理/点/CSS），避免跨端数字误读｜2026-11-16｜自评：未达标（renderscale device-verify）
- [ ] 原生桥 `onError`/`onReady`：若 Attr 侧可能在回调注册前 fire，须像 onReady 一样支持「已发生则重放」，避免 Compose Attr→Event 顺序丢错 ｜2026-11-12｜自评：未达标（dualtrack P1-concurrency）
- [ ] DualTrack/expand View：`executor` 热切后须按当前轨重发 `onReady`（含 backend），不得依赖「Surface 已创建则不再 onSurfaceCreated」｜2026-11-14｜自评：未达标（android-runtime-dualtrack P1-doc-drift）
- [ ] 持 native Surface 的急拆：await 超时也必须停泵并丢弃句柄，禁止超时后继续 tick｜2026-11-14｜自评：未达标（android-runtime-dualtrack P1-concurrency）
- [ ] 删除/改写 kmp-wgpu-native 着色器源后，须同单或明确门禁更新入仓 `.so`/wasm，并对照产物字符串或行为验收；禁止只改 Rust 源却宣称仓库无官方席｜2026-11-14｜自评：未达标（delete-five-scenes P1-plan-soft-drift）
- [ ] 同窗 DualTrack（wgpu↔EGL）落地后，README/runbook 不得残留 `LITE_PENDING` 当现行；切轨占窗释放须真机证｜2026-11-15｜自评：未达标（ohos-lite-adr0011 P1-doc-drift + device-verify）
- [ ] Demo 仅换 fragmentShader 时不应清空 onReady/backend 展示；切 executor 与切片元语义不同｜2026-11-15｜自评：未达标（demo-acd-samples P2）
- [ ] Runtime 错误桥：`emit`+`Result`/`throw` 与宿主 `drain` 只能选一侧 fire，避免同一次失败双发 onError｜2026-11-15｜自评：未达标（wgpu-error-to-business P2-duplicate-onerror）
- [ ] H5 Runtime 主线程回退须与 Worker 同契约透传 tick/lastError/drain，禁止只接 Worker 路径｜2026-11-15｜自评：未达标（wgpu-error-to-business P1-plan-soft-drift）

## 已晋升

- [x] 鸿蒙 NAPI 新导出必须同步 `types/lib*/index.d.ts`｜2026-11-16｜→ `review_heuristics` 卡 F §6
- [x] Kotlin/JS 禁止 `js("ident").asDynamic()` 赋浏览器全局｜2026-11-12｜→ 卡 F §7
- [x] expand View `executor` 切换须对称 teardown（含 Worker/Offscreen）｜2026-11-13｜→ 卡 F §1
- [x] Offscreen Worker 异步 init 失败须与主线程同契约｜2026-11-13｜→ 卡 F §2
- [x] 先 fragment 再 executor：拒非本轨方言；FFI `catch_unwind`｜2026-11-14｜→ 卡 F §3
- [x] DualTrack 性能对比须标注端与分辨率策略；模拟器 fps 不作真机结论｜2026-11-14｜→ 卡 F §4（`renderScale` 细项仍见候选未达标）
- [x] C++ Shadertoy wrap 与 Kotlin SoT 双源须 changelog 同步义务｜2026-11-15｜→ 卡 F §5
