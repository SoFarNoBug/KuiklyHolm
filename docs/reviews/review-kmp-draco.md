# Review · kmp-draco

- 结论：打回
- 依据：Plan [`.cursor/plans/2026-11-18-kmp-draco.md`](../.cursor/plans/2026-11-18-kmp-draco.md)；ADR 0017；kmp-draco ADR 0001；changelog `docs/specs/holm-content-gltf/changelog.md`
- 日期：2026-11-18
- diff：独立仓 `~/Studio/kmp/kmp-draco`（Rust/JNI/Kotlin）；`holm-gltf` KHR_draco + expect/actual；`settings.gradle.kts` `kmp.draco.local`；guide/CONTEXT/ADR

## P0

### P0-build-break 未发布 Maven 坐标却硬依赖
- 证据：[`holm-gltf/build.gradle.kts`](../../holm-gltf/build.gradle.kts) `implementation("io.github.sofarnobug:kmp-draco:0.1.0")`；Central `…/kmp-draco/0.1.0/…` **HTTP 404**；`local.properties`（含 `kmp.draco.local`）被 gitignore，干净树默认**无** includeBuild
- 影响：未设 `kmp.draco.local=true` 且旁路仓不在位时，`:holm-gltf` / `:KuiklyHolmCore` Android 依赖解析失败，干净构建挂
- 处置：必修 — 先 `publishToMavenLocal`（或 Central 0.1.0），**或**在 settings 对缺失坐标默认 includeBuild / 文档+CI 强制 `kmp.draco.local`，再复审

## P1

### P1-device-verify Tokyo / KHR_draco 真机出画
- 证据：Plan 验收「LittlestTokyo 不再空白」；宿主仅有 Rust `tokyo_prim0.drc` 单测 + Android compile，**无**整包 `GltfDocument.parse(Tokyo)` / Demo 真机闭环
- 影响：解码接线代码审可达，观感/71 mesh 耗时/失败 onError 未证
- 处置：必修 → 可执行档 [`device-check-list/kmp-draco.md`](device-check-list/kmp-draco.md)
- 代码审不了：Tokyo 可见可拖；切 gl/wgpu；坏流/缺 so 是否 MODEL_LOAD；包体体感

### P1-plan-soft-drift M0「Android 单元/instrumented」未落地
- 证据：Plan M0 验收「Android 单元或 instrumented：已知字节 → positions」；实现为 **host `cargo test`**，无 androidTest / 设备侧 `Draco.decode` / `GltfDocument.parse` 夹具
- 影响：JNI 构造 `DecodedMesh`/`DecodedAttribute`、loadLibrary 路径未在 Android 运行时自动验证
- 处置：建议（补最小 androidTest 或真机表步骤显式覆盖 JNI）

## P2

### P2-arm64-only-jni
- 证据：仅 `arm64-v8a/libkmp_draco_native.so`（`build-android.sh`）；x86_64 模拟器会 `UnsatisfiedLinkError`
- 影响：与 kmp-wgpu M0 同构；非真机 arm64 联调易误判
- 处置：可选（文档标明仅 arm64，或补 abi）

### P2-skinning-attrs-ignored
- 证据：Tokyo prim attrs 含 `JOINTS_0`/`WEIGHTS_0`；[`GltfGlbParser`](../../holm-gltf/src/commonMain/kotlin/com/jlj/kuiklybase/holm/gltf/GltfGlbParser.kt) 只取 POSITION/NORMAL/TEXCOORD_0
- 影响：可能「有几何但姿态/蒙皮不对」；非 Plan 静默空白类
- 处置：可选（已知遗留；非本波）

### P2-decodedmesh-texcoord-ambiguous
- 证据：[`DecodedMesh.texCoords`](../../../../kmp-draco/kmp-draco/src/commonMain/kotlin/com/jlj/kmp/draco/DecodedMesh.kt) 取首个 `AttrType.TEX_COORD`；Tokyo 有 TEXCOORD_0/1
- 影响：公开 getter 可能拿错套 UV；`holm-gltf` 走 uniqueId 不受影响
- 处置：建议（getter 标明「首个」或按 uniqueId API 推荐）

## 规范候选观察

无。

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/kmp-draco.md`](device-check-list/kmp-draco.md)。

## changelog

请确认 `docs/specs/holm-content-gltf/changelog.md` 已含 kmp-draco / ADR 0017 条目（审核员不代写）。整改后请 append 审查回执行。

## 待补桶

无。
