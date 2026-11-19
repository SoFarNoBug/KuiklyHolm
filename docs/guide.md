# Holm · 接入指南

给**已决定接入**的工程同学。产品定位与最小示例见根 [README.md](../README.md)。构建 / 起 Demo / 发版 → [开发者文档](../CONTRIBUTING.md)。

## 前置

1. 已有 **Kuikly Compose** 宿主（本库不是独立 App SDK）。
2. 版本与 Kuikly 对齐（当前 Maven 坐标后缀 `-2.31.0`；腾讯 Maven 镜像常需同时配置）。
3. 启动时 **register Provider** 并 `setDefault`（见下）；只引 core、不注册 → **明确失败**。
4. 走 `renderer-wgpu` 时，按端带上 **kmp-wgpu native**（`.so` / Pod / ohpm / wasm），详见 [kmp-wgpu 接入指南](https://github.com/SoFarNoBug/kmp-wgpu/blob/main/docs/guide.md)。

## 依赖

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/") }
}

dependencies {
    // Umbrella：兼容只引一个模块（内部仍须显式 Provider）
    implementation("io.github.sofarnobug:KuiklyHolmCore:1.0.0-2.31.0")

    // 或按需拆包（目标形态）：
    // implementation("io.github.sofarnobug:holm-core:…")
    // implementation("io.github.sofarnobug:holm-renderer-gl:…")
    // implementation("io.github.sofarnobug:holm-renderer-wgpu:…")
}
```

| 你要… | 依赖 |
|---|---|
| 统一岛 API | `holm-core`（或 umbrella `KuiklyHolmCore`） |
| 轻量 GL 出画 | + `holm-renderer-gl` + 注册 `GlBackendProvider` |
| wgpu / WGSL 热换 | + `holm-renderer-wgpu` + `WgpuBackendProvider` + **kmp-wgpu native** |
| Android 最小 glTF 模型 | + `:holm-gltf`（**可选**；不进 umbrella 必选；`KHR_draco` 经 `kmp-draco`，ADR 0017）+ `HolmModelContent` |
| 只要无窗口算力 | **不要扩 Holm** → 直接用 [kmp-wgpu](https://github.com/SoFarNoBug/kmp-wgpu) |

### 怎么选：`holm-renderer-gl` vs `holm-renderer-wgpu`

两轨都是「页面里嵌 GPU 画布」，差别在**着色语言、底层栈、包体**。运行时用 `executor` 切换；没登记的那一轨会 `BLOCKED`，**不会**自动改用另一轨。

| 维度 | `holm-renderer-gl`（`executor=renderer-gl`） | `holm-renderer-wgpu`（`executor=renderer-wgpu`） |
|---|---|---|
| 你写什么 | GLSL（Shadertoy 风格**正文**，库补壳） | WGSL（含 `@fragment fn fs_main`） |
| 底层 | 各端系统 OpenGL / GLES / WebGL | [kmp-wgpu](https://github.com/SoFarNoBug/kmp-wgpu)（Vulkan / Metal / GLES / WebGPU…，以运行时为准） |
| 额外原生库 | **一般不需要**再带一套 Holm 专用 `.so` | **必须**带 `libkmp_wgpu_native`（或 wasm / Pod / ohpm） |
| 包体 | 相对轻：Kotlin + 系统 GL | **明显更重**：见下节「包体积」 |
| 适合 | 包体敏感、已有 GLSL/Shadertoy 资产、只要稳出画 | 要统一 WGSL、跟 WebGPU/wgpu 生态对齐、或已在用 kmp-wgpu |
| 不适合单独扛 | 细粒度 WebGPU 原语、无窗口算力 | 同上——算力请直接用 kmp-wgpu，别指望 Holm 变 GPGPU SDK |

**怎么选（实用）**

1. **默认先 GL**：多数「页面里一块动态背景 / 小特效」够用，接入与包体成本最低。  
2. **需要 WGSL / 与 kmp-wgpu 同栈** 再开 wgpu 轨，并接受约 **数 MB 级** native 增量。  
3. **两轨都要**（像 Demo 双轨对比）：两包都引、两个 Provider 都 register；包体按「GL + wgpu native」叠加。  
4. **只要算不要画**：不要为了 Holm 去引 `holm-renderer-wgpu` → 用 kmp-wgpu Headless。

拆包示例（只 GL、控体积）：

```kotlin
implementation("io.github.sofarnobug:holm-core:…")
implementation("io.github.sofarnobug:holm-renderer-gl:…")
// 不要引 holm-renderer-wgpu，也不要打进 kmp-wgpu native
HolmProviders.register(GlBackendProvider)
HolmProviders.setDefault(GlBackendProvider.ID)
```

> 注意：Maven **umbrella** `KuiklyHolmCore` 会把三包能力打进依赖图，方便狗粮，**不利于**「宿主只要 GL、绝对不带 wgpu native」。要控包体请按上表拆包，或确认宿主打包未链入 `libkmp_wgpu_native`。

### 包体积：`holm-renderer-wgpu` 实际重在哪

体积大头**不是** `holm-renderer-wgpu` 这层 Kotlin 适配，而是它依赖的 **kmp-wgpu native**（Rust + wgpu）。只引 GL 轨、不打包该 native → 不会吃到下表增量。

**本仓库现有产物实测**（2026-11-18；未压缩单文件；宿主 APK/IPA/HAP 还会经压缩 / 多 ABI 叠加，以你方打包为准）：

| 端 / 产物 | 文件 | 约 |
|---|---|---|
| Android arm64 | `libkmp_wgpu_native.so` | **~4.05 MB**（4 243 504 B） |
| 鸿蒙 arm64 | `libkmp_wgpu_native.so` | **~4.0 MB**（4 195 072 B） |
| H5 | `kmp_wgpu_native_bg.wasm` | **~2.32 MB**（2 437 508 B） |
| iOS | `KmpWgpuNative` xcframework | 本地联调见下「iOS xcframework 旁路」；发版以 Pod / 入库 xcframework 实秤为准（数 MB 级） |

读数口径：

| 说法 | 含义 |
|---|---|
| 「加了 holm-renderer-wgpu」 | Kotlin 依赖本身通常远小于上表；**真正胀包的是 native** |
| 「只加 Maven、没打 `.so`/wasm」 | 包体几乎不增，但运行时 `BLOCKED` / 链失败 |
| 「多 ABI」（如 arm64 + armeabi-v7a） | 每个 ABI 各一份 `.so`，增量近似 **线性相加** |
| 「GL vs wgpu 增量」 | GL：走系统图形库，Holm 侧无上表这种数 MB native；wgpu：至少一份上表产物 |

建议宿主在发版流水线里对 **release APK/AAB / IPA / HAP** 做一次「只 GL」vs「GL+wgpu」差分，把结果记进自己的发布说明；上表可作为接入前的数量级预期。

### iOS xcframework 旁路（本仓 Demo / 本地链）

`iosApp.xcodeproj` 的 `FRAMEWORK_SEARCH_PATHS` 指向仓根旁路目录（相对 `iosApp/`）：

- `../kmp-wgpu-native/dist/ios` → `KmpWgpuNative.xcframework`
- `../kmp-draco-native/dist/ios` → `KmpDracoNative.xcframework`（Model / `KHR_draco`）

干净 clone 后若目录不存在，在 **KuiklyHolm 仓根**执行：

```bash
ln -sfn ../kmp-wgpu/kmp-wgpu-native kmp-wgpu-native
ln -sfn ../kmp-draco/kmp-draco-native kmp-draco-native
```

（路径按你本机 sibling 仓布局调整。）Model GLES 的 `onReady.backend` 为 `gles2-model`（与片元 Lite 的 `gles2-ios` 区分）。

## Provider（必做）

业务启动时注册后端，并设默认（示例对齐 Demo）：

```kotlin
import com.jlj.kuiklybase.holm.GlBackendProvider
import com.jlj.kuiklybase.holm.HolmProviders
import com.jlj.kuiklybase.holm.WgpuBackendProvider

HolmProviders.register(GlBackendProvider)
HolmProviders.register(WgpuBackendProvider)
if (HolmProviders.defaultIdOrNull().isNullOrBlank()) {
    HolmProviders.setDefault(GlBackendProvider.ID) // 或 WgpuBackendProvider.ID
}
```

| 端 | 典型落点（对照 Demo） |
|---|---|
| Android / H5 | `holmDemo` → `HolmDemoBootstrap` |
| iOS | `HolmIosBridge` |
| 鸿蒙 | `HolmOhosNative` |

未注册对应 executor、或未 `setDefault` → 切岛时报 `onError(code=BLOCKED, …)`，**不会**静默回落。

## 概念：岛 + 双轨

```text
Kuikly 页面布局
    │  决定岛位置 / 大小
    ▼
HolmSurface（Compose） / KRHolmSurface（原生 viewName）
    │  Attr：contentId / executor / playing / maxFps / renderScale
    │  Event：onReady / onError / onPerf / onShaderCompiled
    ▼
holm-core ──Provider──► renderer-gl（GLSL Shadertoy 正文 + 库补壳）
                    └► renderer-wgpu（WGSL 片元 + kmp-wgpu Present）
           （可选）holm-gltf → Android Model Present
```

- **局部岛**：不替换 Kuikly 主 UI；一页少岛；列表默认不要挂实时重岛。
- **显式选后端**：`executor = renderer-gl | renderer-wgpu`（见 `HolmExecutor`）。
- **只挂 Content**：`HolmShaderContent` / `HolmModelContent`；**禁止**业务再传 `fragmentShader` Attr（ADR 0016）。

---

## API · Compose：`HolmSurface`

包名：`com.jlj.kuiklybase.holm`。viewName：`KRHolmSurface`。

```kotlin
@Composable
fun HolmSurface(
    content: HolmContent? = null,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    maxFps: Int = HolmSurfaceAttr.DEFAULT_MAX_FPS,       // 60
    renderScale: Float = HolmSurfaceAttr.DEFAULT_RENDER_SCALE, // 1f
    executor: String = HolmSurfaceAttr.DEFAULT_EXECUTOR, // renderer-gl
    onReady: ((ReadyParams) -> Unit)? = null,
    onError: ((ErrorParams) -> Unit)? = null,
    onPerf: ((PerfParams) -> Unit)? = null,
    onShaderCompiled: ((ShaderCompiledParams) -> Unit)? = null,
)
```

Declarative 薄壳：`HolmSurfaceView` + `HolmSurfaceAttr` / `HolmSurfaceEvent`。Compose 将 `content` 登记进 `HolmContentRegistry`，Attr 只传 `contentId`（+ 无 Registry 堆访问端的 NVI 桥字段）。

### Content

| 类型 | 含义 |
|---|---|
| `null` / 空 | 清屏，**不**报错 |
| `HolmShaderContent(source, vertexSource?, warpAmount?, scrollSpeed?)` | 片元。GL=Shadertoy **正文**；wgpu=WGSL（含 `@fragment fn fs_main`） |
| `HolmModelContent.fromAssets` / `fromFileUri` / `fromBytes` | 最小 glTF；**Android + iOS + Ohos 真画**（ADR 0018/0019）；H5 `MODEL_UNSUPPORTED` + 清屏 |

`HolmModelContent`：默认 `orbitEnabled=true`（单指拖 yaw/pitch）；`setYaw` / `setPitch` 供业务自写手势；`setLights` 热更新灯光（不重载 glTF）；`lights` 空=unlit，上限 `HolmModelContent.MAX_LIGHTS`（8）。资源：**不做 http**；先验 `assets/`，另有 `file://` 与 `ByteArray`。

### Attr（岛级）

| 参数 | 类型 | 默认 | 含义 |
|---|---|---|---|
| `contentId` | `String` | `""` | Registry id；空=无 Content |
| `playing` | `Boolean` | `true` | `false` 停泵：不再提交 GPU；恢复后继续 |
| `executor` | `String` | `renderer-gl` | `HolmExecutor.GL` / `HolmExecutor.WGPU`；其它值归一成 GL |
| `maxFps` | `Int` | `60` | **期望**提交上限；`0`=不限制；`<0` 按 0。与 `onPerf.fps` **不是同一含义**（见下） |
| `renderScale` | `Float` | `1` | GPU 缓冲相对**当前平台基线**的缩放；与 `maxFps` 正交。`≤0` / 非有限 → `1`；夹紧 `(0, 1]`（禁止超采样） |

**`renderScale` 基线（因端而异）**

| 端 | 基线像素 |
|---|---|
| Android / Ohos | 物理 Surface px |
| iOS renderer-gl | 点像素 |
| iOS renderer-wgpu | `points × nativeScale` |
| H5 | CSS px |

**`maxFps` 平台差异（勿当 bug）**

| 端 | 行为 |
|---|---|
| H5 | rAF + `nextDue` **跳过提交**；`onPerf` 大致贴档 |
| Ohos | ExpectedFrameRate + 低档（如 30）软件间隔；≥55 / 0 交给面板 vsync |
| Android | `Surface.setFrameRate` **仅提示**；`RENDERMODE_CONTINUOUSLY` 下设 30 仍可能每 vsync 提交 → `onPerf` 仍可 ~60 |
| iOS | DisplayLink 侧按档泵；以实测 `onPerf` 为准 |

若 Android 要「设 30 则真实提交也到 ~30」，需改 `WHEN_DIRTY` + 自建时钟（**当前未做**）。

### Event

| 事件 | 载荷 | 何时 |
|---|---|---|
| `onReady` | `ReadyParams(backend, quality)` | 岛 / 后端就绪；热换片元**不保证**重发 |
| `onError` | `ErrorParams(code, message)` | 失败可见：缺 Provider、缺 native、编译/链接失败、tick 失败等 |
| `onPerf` | `PerfParams(fps, frameMs)` | 约 **1Hz**（iOS 窗约 0.5s）上报；须挂回调才会统计/上报 |
| `onShaderCompiled` | `ShaderCompiledParams(ok, wrapMs, compileMs, executor)` | **每次**片元装载尝试结束（成败皆可）；与 `onError` 可双发（ADR 0012） |

#### `onReady`

| 字段 | 含义 |
|---|---|
| `backend` | 运行时后端 id（如 `gles2` / `gles2-ohos` / `webgl` / `wgpu-m0…`）；**以回调为准**，勿写死端=某 API |
| `quality` | 质量档占位（常见 `auto`） |

#### `onError`（常见 code）

| code | 典型原因 |
|---|---|
| `BLOCKED` | 未 register / 未 setDefault；或 wgpu 缺 native / 不可用（**不**静默回 GL） |
| `MODEL_UNSUPPORTED` | H5 挂了 `HolmModelContent`（ADR 0018/0019：Android+iOS+Ohos 已真画） |
| `MODEL_LOAD` | Android 读 assets / 解析 glTF 失败 |
| `SHADER_WRAP` / `SHADER_COMPILE` / `SHADER_LINK` / `SHADER_CUSTOM` | 片元包壳或编译/装载失败 |
| `TICK` | Present / 泵帧回合失败 |
| 其它平台码 | 如 H5 `CREATE_SESSION` / `NO_WASM_URL`；以 `message` 为准 |

业务应将错误做成 sticky UI；**不要**仅凭后续 `onPerf` 自动清错（Demo 对部分码有策略，接入方自行约定）。

#### `onPerf`（真实提交 FPS）

**定义：** 约 1 秒滑动窗口内，**实际完成 draw / tick 提交**的次数。

```text
fps     = 窗口内提交次数 / 窗口秒数
frameMs ≈ 1000 / fps
```

| 要点 | 说明 |
|---|---|
| 计量对象 | **Present / 提交 FPS**（业界常用层），不是 Display 合成帧率，也不是 GPU timestamp query |
| 何时 +1 | 真正 clear/draw 或 Runtime `tick` **成功之后**；限帧跳过、未 ready、`playing=false`、tick 失败 **不计** |
| 与 `maxFps` | `maxFps` = 期望上限；`onPerf.fps` = 实测提交率。Android 上二者常不一致（见上表）——**统计没错** |
| `frameMs` | 由窗口平均 fps **反推**，适合 HUD；不是单帧真值，也无 1% low / p99 |
| 读数 | 须挂 `onPerf`；等 ≥1s（或 iOS ≈0.5s）再读；首窗可能为空 |

适合：对比 shader / 后端成本、看限帧是否真跳提交。  
不适合单独回答「人眼是否合帧 / 偶发卡顿」——那要 display 侧或帧时间百分位（本 API 未提供）。

#### `onShaderCompiled`

| 字段 | 含义 |
|---|---|
| `ok` | 本次装载是否成功 |
| `wrapMs` | 包壳墙钟；**renderer-wgpu 恒为 0** |
| `compileMs` | GL：compile+link；wgpu：pipeline 墙钟；未跑到的步骤为 0 |
| `executor` | 本次尝试对应的执行器字符串 |

热换与 Surface/上下文恢复重编都会触发；**不要**把耗时塞进 `onReady` / `onPerf`。

---

## 最小接入示例

```kotlin
import androidx.compose.runtime.Composable
import com.jlj.kuiklybase.holm.HolmExecutor
import com.jlj.kuiklybase.holm.HolmShaderContent
import com.jlj.kuiklybase.holm.HolmSurface
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.ui.Modifier

@Composable
fun HolmIsland(fs: String) {
    HolmSurface(
        content = HolmShaderContent(source = fs),
        modifier = Modifier.fillMaxSize(),
        executor = HolmExecutor.WGPU, // 或 HolmExecutor.GL
        playing = true,
        maxFps = 60,
        onReady = { r -> /* r.backend */ },
        onError = { e -> /* e.code / e.message */ },
        onPerf = { p -> /* p.fps / p.frameMs — 真实提交率 */ },
        onShaderCompiled = { c -> /* c.ok / wrapMs / compileMs */ },
    )
}
```

- GL 路径喂 **GLSL Shadertoy 正文**；wgpu 路径喂 **WGSL**（两端样本见 Demo `NeonTunnelShaders` 等）。
- 完整双轨：`holmDemo` → page `holm_dualtrack`；Model：`holm_model`（Android + iOS，ADR 0018）。

## 能力矩阵

| | Android | iOS | 鸿蒙 | H5 |
|---|---|---|---|---|
| renderer-gl | ✅ | ✅ | ✅ | ✅ |
| renderer-wgpu | ✅ | ✅ | ✅ | ✅ |
| ShaderContent 热换 | ✅ | ✅ | ✅ | ✅ |
| ModelContent（glTF） | ✅ GL=GLES；wgpu=`ModelPass` | ✅ GLES + Metal ModelPass（ADR 0018） | ✅ GLES + ModelPass（ADR 0019） | BLOCKED |
| Provider 未设 | 失败 | 失败 | 失败 | 失败 |
| `onPerf` 真实提交 | ✅ | ✅ | ✅ | ✅ |

wgpu 路径上各端**实际图形后端**（Vulkan / Metal / GLES / WebGPU…）由 kmp-wgpu 选择，以 `onReady.backend` / 运行时 id 为准。

## 接入避坑

| 坑 | 现象 | 怎么避 |
|---|---|---|
| 只引 core、不 register | `BLOCKED` | 启动时 `HolmProviders.register` + `setDefault` |
| wgpu 忘 native | `BLOCKED` / 链失败 | 按 [kmp-wgpu guide](https://github.com/SoFarNoBug/kmp-wgpu/blob/main/docs/guide.md) 补 `.so` / Pod / ohpm / wasm |
| 用 `maxFps` 当实测 FPS | Android 设 30 仍觉得「统计错」 | 读 `onPerf.fps`；Android 限帧仅为提示 |
| GL/WGSL 方言喂错轨 | 编译失败 / 红字 | Content 源码与 `executor` 方言一致；热切注意方言 |
| 业务仍传 `fragmentShader` Attr | 编译失败 / 无效 | 改用 `HolmShaderContent`（ADR 0016） |
| H5 挂 Model | `MODEL_UNSUPPORTED` | Android+iOS+Ohos 真画（ADR 0018/0019）；H5 仍 BLOCKED |
| 只用 Shader 却引 `:holm-gltf` | 包体变大 | Model 可选依赖；Shader 宿主可不引 |
| 列表里挂多岛实时泵 | 掉帧 / 耗电 | 局部少岛；列表默认禁实时重岛 |
| 无窗口 GPGPU 塞进 Holm | API 不对口 | 直接用 kmp-wgpu Headless |
| 把系统玻璃当 Holm 完成 | 验收争议 | 非目标；勿冒充四端一致底座 |

## 仓库指路

| 路径 | 角色 |
|---|---|
| 根 [README.md](../README.md) | 路人 / 产品定位 / 最小示例 |
| 本文件 | **接入手册 + API 全表** |
| [CONTRIBUTING.md](../CONTRIBUTING.md) | 维护：起 Demo / 发版 |
| [`holmDemo/`](../holmDemo/) | 双轨隧道 + Model 样本 |
| [`holm-core/`](../holm-core/) | 岛 API / Content / Providers |
| [`holm-gltf/`](../holm-gltf/) | 可选 GLB 解析（CPU）；Android 上 `KHR_draco` → `kmp-draco` |
| [`../kmp-draco`](../../kmp-draco/) | 独立仓 Draco 比特流解码（`io.github.sofarnobug:kmp-draco`；旁路仓在则默认 includeBuild；`kmp.draco.local=false` 走 Maven） |
| [`holm-renderer-gl/`](../holm-renderer-gl/) | Lite GL |
| [`holm-renderer-wgpu/`](../holm-renderer-wgpu/) | wgpu Present 薄适配 |
| [`docs/product/`](./product/) | 产品决策 SoT |
| [`docs/adr/`](./adr/) | 架构决策 |
