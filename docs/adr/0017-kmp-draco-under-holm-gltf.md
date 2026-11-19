# ADR 0017: kmp-draco 承接 KHR_draco（holm-gltf 硬依赖）

- Status: Accepted
- Date: 2026-11-18
- Branch: `feat/holm-model-content-android`（计划接线；实现另单）

## Context

ADR 0016 关闭 O3：自研最小 glTF，`holm-gltf` 做 CPU 解析，Model Present 在 Android。样本 `LittlestTokyo.glb` 使用 **`KHR_draco_mesh_compression`** 且无普通 `bufferView` 几何，纯 Kotlin 解析无法出网格 → 空白。需要解码原语，且不得把 Draco/C++/Filament 塞进 `kmp-wgpu` 或 `holm-core`。

审讯（2026-11-18）锁定独立仓 `kmp-draco`（平行 ADR 0015 的 `kmp-wgpu` 故事）。

## Decision

1. **原语**：消费 **`io.github.sofarnobug:kmp-draco`**（仓 `~/Studio/kmp/kmp-draco`；身份见该仓 ADR 0001）。Holm **不**自养第二套 Draco。
2. **接线**：`:holm-gltf` **硬依赖** `kmp-draco`。解析到 extension 的 Draco 载荷字节即同步 `decode`；结果按 accessor 语义拼回现有网格 IR。不做 Provider SPI（本波）。
3. **失败**：解码失败或 required extension 无法展开 → **上抛 / onError**，禁止静默空场景（修 Tokyo「空白」类问题）。
4. **边界不变**：`kmp-draco` 不懂 glTF；`kmp-wgpu` / ModelPass 仍只收已展开属性；`holm-core` 不直接依赖 Draco。
5. **M0 平台**：与 kmp-draco M0 一致，先 **Android**；其它端 Model + Draco 仍可 BLOCKED / fail-loud。
6. **联调**：坐标 `io.github.sofarnobug:kmp-draco`。未发 Central 前：旁路仓 `../kmp-draco` **存在则默认** `includeBuild`；显式 `kmp.draco.local=false` 才解析 Maven（可先 `kmp-draco/publish-maven.sh local`）。键名对齐 `kmp.wgpu.local` 习惯。

## Considered Options

- 解码器 SPI / Provider 注入 → 拒绝（换简单硬依赖；宿主不引 `holm-gltf` 即无 Draco）
- Draco 进 `kmp-wgpu-native` → 拒绝（wgpu 原语不懂压缩格式；GL 轨也要解）
- Google C++ 内核挂在 Holm 仓内 → 拒绝（原语应独立；且审讯选 Rust 身份）

## Consequences

- 引 `:holm-gltf` 的 App 包体增加 Android Draco native；只用 Shader 的宿主仍可不引。
- 须改 `holm-gltf` 解析路径 + Demo Tokyo 验收；补 guide / changelog / 真机表。
- 依赖该仓发版或 `includeBuild` 后方可在 CI/真机闭环。

## Relates

- ADR 0016（Content + 最小 glTF）
- ADR 0013 / 0015（kmp-wgpu 原语与独立仓——平行故事，非同一库）
- kmp-draco ADR 0001
