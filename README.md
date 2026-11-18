# Holm（KuiklyHolm）

**Kuikly 页面里，嵌一块 GPU 画布。**

让你可以在Kuikly页面中用着色器代码（shader）来画——动态背景、小可视化、全屏感特效等（就是使用gpu渲染shader）。Android / iOS / 鸿蒙 / 浏览器，同一套接法。

谁干什么：

- **放哪、多大** → 页面布局
- **画什么** → 你的 shader
- **怎么画到各端屏幕上** → Holm（轻量 OpenGL，或 [kmp-wgpu](https://github.com/SoFarNoBug/kmp-wgpu)）

它**不是**整页游戏引擎，也**不是**内置「毛玻璃 / 粒子」特效包——画面内容要自己写（仓库 Demo 里有霓虹隧道等例子可抄）。一块页面里少挂几块就好，列表里一般不要挂一直在刷的重特效。


|         |                                                                                         |
| ------- | --------------------------------------------------------------------------------------- |
| Maven   | `io.github.sofarnobug:KuiklyHolmCore:1.0.0-2.31.0`                                      |
| 模块      | `holm-core` · `holm-renderer-gl` · `holm-renderer-wgpu`（也可只引 umbrella `KuiklyHolmCore`） |
| 包名      | `com.jlj.kuiklybase.holm`                                                               |
| 端       | Android / iOS / 鸿蒙 / H5                                                                 |
| Demo    | `:holmDemo` · 页面名 `holm_dualtrack`                                                      |
| License | 见 [LICENSE](LICENSE)                                                                    |


> **路人读本页。** 怎么接到工程、每个参数什么意思 → [docs/guide.md](docs/guide.md)。怎么起 Demo / 发版 → [开发者文档](CONTRIBUTING.md)。

---

## 为什么需要 Holm？

用 Kuikly 做 App 时，偶尔会想在页面里嵌一块**真的用显卡画**的区域。常见两条岔路都不理想：

1. **每端自己接** Surface / OpenGL / Metal —— 同一套特效要写四遍桥
2. **上完整游戏 / 自绘引擎** —— 只为页面里一块背景太重，还容易和现有 UI 抢主场

Holm 的做法：给你一个组件 `HolmSurface`。你像放普通控件一样定位置和大小；把着色器源码传进去；在启动时登记要用哪种绘制后端。后端没配好就**明确报错**，不会假装画成功。

文档里把这块矩形叫 **「岛」**（局部 GPU 区域）：周围仍是 Kuikly UI，只有岛内走 GPU。

```text
Kuikly 页面（布局决定岛在哪、多大）
    │  HolmSurface
    ▼
holm-core  ──登记后端──►  holm-renderer-gl     （偏轻量，GLSL）
                      └►  holm-renderer-wgpu   （WGSL，底下是 kmp-wgpu）
                                │
                                ▼
                         各端真实图形接口（Vulkan / Metal / GLES / WebGPU …）
```

如果只想「算完拿数组、不要嵌在页面里画」，请直接用 [kmp-wgpu](https://github.com/SoFarNoBug/kmp-wgpu)，不必绕 Holm。

---

## Holm 能干什么？交到哪一步？


| 你想要的                        | 状态      | 说明                                                   |
| --------------------------- | ------- | ---------------------------------------------------- |
| 页面里嵌一块 GPU 画布，自己写画面         | **已有**  | 组件名 `HolmSurface`                                    |
| 同一套 API，可选 OpenGL 或 wgpu    | **已有**  | 参数 `executor`；Demo 里可来回切                             |
| Android / iOS / 鸿蒙 / 浏览器都能画 | **已有**  | 见下方能力表                                               |
| 知道着色器编译花了多久、现在大概几帧          | **已有**  | `onShaderCompiled` · `onPerf`                        |
| 库自带毛玻璃 / 粒子等成品特效            | **非目标** | 自己写或抄 Demo 样本                                        |
| 整页都用 Holm 画、不用 Kuikly       | **非目标** | 只适合局部一块                                              |
| 不做界面、只在后台用 GPU 算数           | **非目标** | 用 [kmp-wgpu](https://github.com/SoFarNoBug/kmp-wgpu) |
| 列表滑动时每行都做重度实时特效             | **非目标** | 费电且易掉帧；别这么用                                          |


现在稳定交付的就是：**一个页面组件 + 两种可选绘制后端**（轻量 GL，或走 kmp-wgpu 的 WGSL）。

---

## 模块怎么选

```text
页面里要这块 GPU 画布     →  引 KuiklyHolmCore（或下面三包自选）
只想用轻量 OpenGL        →  holm-core + holm-renderer-gl（包体更友好）
还要 WGSL / 现代 wgpu    →  再加 holm-renderer-wgpu，并带上 kmp-wgpu 原生库（约数 MB）
只要后台算数、不要画布  →  别用 Holm，直接用 kmp-wgpu
```


| 模块                     | 你得到什么                                         |
| ---------------------- | --------------------------------------------- |
| **holm-core**          | 组件 API、后端登记（`HolmProviders`）                  |
| **holm-renderer-gl**   | 轻量 OpenGL 绘制实现                                |
| **holm-renderer-wgpu** | 基于 kmp-wgpu 的 WGSL 绘制实现（体积大头在 native，见 guide） |
| **KuiklyHolmCore**     | 上面几包打包成一个依赖（启动时仍要**登记后端**；要极致控包请拆包只引 GL）      |


**选 GL 还是 wgpu、包体积大概多少**（Android `.so` ≈4 MB / H5 wasm ≈2.3 MB 等）→ [docs/guide.md · 怎么选](docs/guide.md#怎么选holm-renderer-gl-vs-holm-renderer-wgpu)。

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/") }
}

dependencies {
    implementation("io.github.sofarnobug:KuiklyHolmCore:1.0.0-2.31.0")
}
```

启动时告诉 Holm 用哪些后端（必做；忘了会报错 `BLOCKED`，不会偷偷改用别的）：

```kotlin
HolmProviders.register(GlBackendProvider)
HolmProviders.register(WgpuBackendProvider)
HolmProviders.setDefault(GlBackendProvider.ID) // 或 WgpuBackendProvider.ID
```

参数说明、各端注意点 → [docs/guide.md](docs/guide.md)。

---

## 最小例子

把着色器字符串 `fs` 传进去即可（完整可运行工程见 Demo）：

```kotlin
@Composable
fun HolmIsland(fs: String) {
    HolmSurface(
        content = HolmShaderContent(source = fs),
        modifier = Modifier.fillMaxSize(),
        executor = HolmExecutor.WGPU, // 或 HolmExecutor.GL（OpenGL）
        playing = true,
        onReady = { /* 画布就绪 */ },
        onError = { /* 失败时看这里，不会假装画成功 */ },
        onPerf = { /* 大约每秒回报一次真实刷新情况 */ },
    )
}
```

- 选 OpenGL（`HolmExecutor.GL`）：`fs` 写 GLSL（类似 Shadertoy 正文，库会补外壳）  
- 选 wgpu（`HolmExecutor.WGPU`）：`fs` 写 WGSL（需要带 `@fragment fn fs_main`）  
- 想看成品效果：跑 `holmDemo`，打开页面 `holm_dualtrack`（霓虹隧道等）；Android 模型页 `holm_model`

---

## 能力矩阵


|                        | Android | iOS  | 鸿蒙   | H5   |
| ---------------------- | ------- | ---- | ---- | ---- |
| renderer-gl            | ✅       | ✅    | ✅    | ✅    |
| renderer-wgpu          | ✅       | ✅    | ✅    | ✅    |
| 片元热换                   | ✅       | ✅    | ✅    | ✅    |
| Provider 未设 / 缺 native | 失败可见    | 失败可见 | 失败可见 | 失败可见 |


wgpu 路径上各端**实际**推像素的后端（Vulkan / Metal / GLES / WebGPU…）由 kmp-wgpu 选择，以运行时 `onReady.backend` 为准——**不要写死**「某端 = 某 API」。说明见 [kmp-wgpu README](https://github.com/SoFarNoBug/kmp-wgpu#readme)。

---

## 常见问题（路人）

**Q. 这和我自己在 Android / iOS 各接一套 OpenGL、Metal 有什么差别？**  
A. 业务只对接一个 Kuikly 组件；四端差异收在库里。布局继续用 Compose，不用为这一块特效拆四套桥。

**Q. Holm 是不是毛玻璃 / 粒子成品库？**  
A. **不是。** 它只提供「页面里的 GPU 画布」；画面要自己写着色器，或从 Demo 样本改。

**Q. 和 kmp-wgpu 什么关系？**  
A. Holm = **嵌在 Kuikly 页面里画**。底层若选 wgpu，会用到 [kmp-wgpu](https://github.com/SoFarNoBug/kmp-wgpu)。如果只要 GPU 算数、不要这块画布，直接用 kmp-wgpu 即可。

**Q. 我把最高帧率设成 30，回调里为什么还显示大约 60？**  
A. 回调里的数字是「实际画了多少次」，不是「你设的上限」。在 Android 上上限目前多半只是提示系统，不保证真限到 30。详见 [guide](docs/guide.md#onperf真实提交-fps)。

**Q. 能当完整游戏引擎用吗？**  
A. 不适合。它适合页面里**局部**一块 GPU 画面；整页自绘、复杂 3D 场景不是目标。

**Q. 我想动手接到工程，看哪？**  
A. → [docs/guide.md](docs/guide.md)。想跑仓库自带 Demo / 发版 → [开发者文档](CONTRIBUTING.md)。

---

## 仓库里有什么


| 路径                                           | 角色             |
| -------------------------------------------- | -------------- |
| `[holm-core/](holm-core/)`                   | 组件 API、后端登记    |
| `[holm-renderer-gl/](holm-renderer-gl/)`     | 轻量 OpenGL 实现   |
| `[holm-renderer-wgpu/](holm-renderer-wgpu/)` | wgpu / WGSL 实现 |
| `[KuiklyHolmCore/](KuiklyHolmCore/)`         | 打包成一个 Maven 依赖 |
| `[holmDemo/](holmDemo/)`                     | 可运行示例（含霓虹隧道）   |
| `[docs/guide.md](docs/guide.md)`             | 接入手册           |
| `[CONTRIBUTING.md](CONTRIBUTING.md)`         | 给维护本仓的人        |


---

接入细节与避坑 → **[docs/guide.md](docs/guide.md)**  
构建 / 发版 / 共建 → **[开发者文档](CONTRIBUTING.md)**