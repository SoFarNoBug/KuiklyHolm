# KuiklyHolm（Holm）

**页面上开一块 GPU 岛：你定位置与大小，丢进上色 shader，局部出帧。**

Holm 是面向 **Kuikly Compose** 的**通用渲染底座**（shader-in / surface-out）：不绑死某一种特效，不做整页自绘引擎。岛少、局部用；列表默认不要挂实时重岛。

| 项 | 值 |
|---|---|
| 包名 | `com.jlj.kuiklybase.holm` |
| 三包 | `holm-core` · `holm-renderer-gl` · `holm-renderer-wgpu` |
| Umbrella | `KuiklyHolmCore`（兼容「只引一个 Core」） |
| Maven | `io.github.sofarnobug:KuiklyHolmCore:1.0.0-2.31.0` |
| Demo | `:holmDemo` · 主推「霓虹隧道」样本 |
| 端 | Android / iOS / 鸿蒙 / H5 |

> **路人 / 接入读本页。** 起 Demo、发版、ADR → [CONTRIBUTING.md](CONTRIBUTING.md)。术语 → [CONTEXT.md](CONTEXT.md)。

---

## 它解决什么问题？

在 Kuikly 页面里，有时需要一块**真正跑在 GPU 上**的局部表面：全屏感片元、程序化背景、轻量可视化——又不想为此换掉整套 UI 框架。

Holm 的约定：

1. **布局决定岛**：宽高、位置由 Compose / 页面布局说了算  
2. **业务喂 shader**：把片元（及可选顶点）交给岛  
3. **显式选后端**：`renderer-gl`（轻量 GL 族）或 `renderer-wgpu`（wgpu / WGSL 热换）  
4. **必须设 Provider**：只引 core、不注册后端 → **明确失败**，不会假装出画  

底层算力 / Present 原语由独立仓 **[kmp-wgpu](https://github.com/SoFarNoBug/kmp-wgpu)** 承担；`holm-renderer-wgpu` 是其上的薄适配，不把 Holm 扩成通用 GPU 算力 SDK。

```text
Kuikly 页面
    │  HolmSurface（岛）
    ▼
holm-core  ──Provider──►  holm-renderer-gl
                      └►  holm-renderer-wgpu
                                │
                                ▼
                         kmp-wgpu / kmp-wgpu-surface
                                │
                                ▼
                         kmp_wgpu_native（Rust + wgpu）
```

---

## 非目标

| 不做 | 说明 |
|---|---|
| 玻璃 / 粒子「产品身份」 | Holm 是底座，不是某一款特效 SDK |
| 官方内置特效目录（五席等） | 已废；样本放 Demo（如隧道），业务自带 WGSL/GLSL |
| 整页自绘 / 取代 Kuikly | 只做**局部岛** |
| 静默假画 | 无 Provider / 缺 native → 报错 |
| 把通用 GPGPU API 塞进 Holm | 无窗口算力走 [kmp-wgpu](https://github.com/SoFarNoBug/kmp-wgpu) |

---

## 前置条件

1. 已有 **Kuikly Compose** 宿主（本库不是独立 App SDK）。  
2. 版本与 Kuikly 对齐（当前坐标后缀 `-2.31.0`；腾讯 Maven 镜像常需同时配置）。  
3. 启动时 **register Provider**（见下）；仅依赖 umbrella 时仍须按端完成注册 / 初始化。  
4. 走 `renderer-wgpu` 时，按端带上 **kmp-wgpu native**（`.so` / Pod / ohpm / wasm），详见 [kmp-wgpu 接入指南](https://github.com/SoFarNoBug/kmp-wgpu/blob/main/docs/guide.md)。

---

## 添加依赖

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

未发版或联调源码时，用本仓模块依赖；`renderer-wgpu` 默认可走 Maven `io.github.sofarnobug:kmp-wgpu*:0.1.0`（本地尖刺见 CONTRIBUTING）。

---

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

各端落点（Android `Application` / iOS 桥 / 鸿蒙壳 / H5 入口）见 Demo 与 [CONTRIBUTING.md](CONTRIBUTING.md)。

---

## 最小接入 · HolmSurface

```kotlin
import androidx.compose.runtime.Composable
import com.jlj.kuiklybase.holm.HolmExecutor
import com.jlj.kuiklybase.holm.HolmSurface
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.ui.Modifier

@Composable
fun HolmIsland(fs: String) {
    HolmSurface(
        modifier = Modifier.fillMaxSize(),
        executor = HolmExecutor.WGPU, // 或 GL
        fragmentShader = fs,
        playing = true,
        // onReady / onError / onPerf / onShaderCompiled 按需接
    )
}
```

- **`executor`**：选 `renderer-gl` / `renderer-wgpu`（API 以 `HolmExecutor` 为准）  
- **`fragmentShader`**：GL 路径多为 GLSL 风格正文；wgpu 路径为 **WGSL**（两端样本见 Demo `NeonTunnelShaders` 等）  
- 无片元 / 编译失败：走 `onError`，不要假设会静默出画  

完整双轨 Demo：`holmDemo` → page `holm_dualtrack`（隧道 / 其它样本 + 切后端）。

---

## 模块怎么选

| 你要… | 依赖 |
|---|---|
| 统一岛 API | `holm-core`（或 umbrella `KuiklyHolmCore`） |
| 轻量 GL 出画 | + `holm-renderer-gl` + 注册 `GlBackendProvider` |
| wgpu / WGSL 热换 | + `holm-renderer-wgpu` + `WgpuBackendProvider` + **kmp-wgpu native** |
| 只要无窗口算力 | **不要扩 Holm** → 直接用 [kmp-wgpu](https://github.com/SoFarNoBug/kmp-wgpu) |

---

## 能力一览（四端 Present）

| | Android | iOS | 鸿蒙 | H5 |
|---|---|---|---|---|
| renderer-gl | ✅ | ✅ | ✅ | ✅ |
| renderer-wgpu | ✅ | ✅ | ✅ | ✅ |
| 片元热换 | ✅ | ✅ | ✅ | ✅ |
| Provider 未设 | 失败 | 失败 | 失败 | 失败 |

wgpu 路径上各端**实际图形后端**（Vulkan / Metal / GLES / WebGPU…）由 kmp-wgpu 选择，以运行时 `backend` / `backendId` 为准；鸿蒙常见为 **GLES**（详见 [kmp-wgpu README](https://github.com/SoFarNoBug/kmp-wgpu#readme)）。

---

## 文档地图

| 文档 | 给谁 |
|---|---|
| 本 README | 路人 / 接入 |
| [CONTEXT.md](CONTEXT.md) | 术语 |
| [docs/product/](docs/product/) | 产品决策 SoT |
| [docs/adr/](docs/adr/) | 架构决策（0007 底座 · 0008 三包 · 0013/0015 kmp-wgpu） |
| [CONTRIBUTING.md](CONTRIBUTING.md) | 维护：起 Demo / 发布 / 联调 |

---

## License

见 [LICENSE](LICENSE)。
