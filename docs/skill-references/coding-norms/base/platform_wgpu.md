doc_version: 20261112_1
layer: L2

# wgpu / WebGPU 平台规范（L2）

> **范围**：以 **wgpu**（Rust 绑定）编写 GPU 程序时的跨端通用硬约束；语义对齐 **W3C WebGPU / WGSL**。本文件属 **可移植 L2**。  
> **不含**：业务产品是否允许静默回落 GLES、Holm Attr/桥接命名、具体 crate 目录（→ **各项目 L3**）。  
> **来源（官方背书）**：  
> 1. [W3C WebGPU](https://www.w3.org/TR/webgpu/)（`GPUDevice` / `GPUQueue` / `GPUCanvasContext`）  
> 2. [W3C WGSL](https://www.w3.org/TR/WGSL/)（着色语言）  
> 3. [wgpu · learning_wgpu](https://wgpu.rs/doc/wgpu/documentation/getting_started/learning_wgpu/index.html)  
> 4. [wgpu · Dos and Don’ts](https://wgpu.rs/doc/wgpu/documentation/best_practices/dos_and_donts/index.html)  
> 5. [wgpu `Device` / `Queue` / `Surface` rustdoc](https://docs.rs/wgpu/latest/wgpu/)（与 crate 版本同步）  
> 未列条目默认跟上述来源；冲突序 **L3 > L2 > L1**。wgpu API 与 WebGPU 概念对应时，**行为争议以 W3C WebGPU 为准**，Rust 方法名以当前 `wgpu` rustdoc 为准。

## 硬约束

### 1. 角色分工：Device 建资源，Queue 提交

来源：[learning_wgpu · Device and Queue](https://wgpu.rs/doc/wgpu/documentation/getting_started/learning_wgpu/index.html)、[W3C `GPUDevice` / `GPUQueue`](https://www.w3.org/TR/webgpu/)

- **`Device`**：几乎所有 GPU 资源创建（buffer、texture、pipeline、encoder 等）。
- **`Queue`**：提交 `CommandBuffer`、以及 `write_buffer` / `write_texture` / present 等排队工作。
- 典型生命周期：启动时创建 **一份** `Device` 与 **一份**（主）`Queue`，不要按帧重复 `request_device`。
- `Device` 携带的 `Features` / `Limits` 由请求时钉死，并独立于底层硬件「实际还能多什么」——超限即失败，禁止假定未请求的扩展可用。

### 2. Surface / Canvas：配置 → 取纹理 → 提交 → 呈现

来源：[W3C `GPUCanvasContext`](https://www.w3.org/TR/webgpu/)、[wgpu `Surface`](https://docs.rs/wgpu/latest/wgpu/struct.Surface.html)、[wgpu `Queue::present`](https://docs.rs/wgpu/latest/wgpu/struct.Queue.html)

- 呈现目标须先 **configure**（WebGPU：`GPUCanvasContext.configure`；wgpu：`Surface::configure`），再每帧取得当前可绘纹理。
- 帧路径（概念序）：`get_current_texture`（或 WebGPU `getCurrentTexture`）→ 编码渲染 → **`Queue::submit`** → **`Queue::present`**（wgpu；须在相关 GPU 工作提交之后）。
- **禁止**在仍有存活的旧 `SurfaceTexture` 引用时 `configure`（wgpu rustdoc：会 panic / 竞态）；resize / 重建交换链前结束对旧纹理的使用。
- `configure` 时宽或高为 **0**、或请求不支持的格式 / 色彩空间 → 失败或 panic（见 `Surface::configure`）；调用方须在配置前用合法尺寸与 `get_capabilities` / 默认配置校验。

### 3. 每帧提交与资源创建（Dos and Don’ts）

来源：[wgpu Dos and Don’ts](https://wgpu.rs/doc/wgpu/documentation/best_practices/dos_and_donts/index.html)

- **Don’t**：每帧创建大量 buffer / texture（压迫分配器与 tracker）。优先合并、大缓冲切片、图集 / 数组纹理。
- **Don’t**：每帧多次滥发 `Queue::submit`。允许一次 submit 多个 `CommandBuffer`，但 **submit 次数宜限制在每帧约 1–5 次**。
- **Don’t**：为更新数据临时 map 短命 buffer 当默认路径。优先 **`Queue::write_buffer` / `Queue::write_texture`**；大量生成数据可考虑回收 staging 池（指南原文）。
- **Do**：按**变更频率**组织 bind group（低频 → 高频，例如 per-frame → per-pass → per-material），减少状态切换。

### 4. Queue 写入语义

来源：[wgpu `Queue::write_buffer`](https://docs.rs/wgpu/latest/wgpu/struct.Queue.html)

- `write_buffer` / `write_texture` **不会**立刻作为独立 GPU 提交完成；传输在**下一次** `Queue::submit` 时、在显式提交的命令之前调度。若只要立刻开跑传输，可 `submit` 空命令列表（官方示例）。
- 写入必须完全在 buffer 界内（`offset + data.len() <= buffer.len()`）。

### 5. 着色语言

来源：[W3C WGSL](https://www.w3.org/TR/WGSL/)

- 经 WebGPU / wgpu 下发的 GPU 程序（render / compute shader）以 **WGSL** 为语言真源。
- 与 GLSL 等其它着色语言的双写、转译、同源策略 → **各项目 L3**（本 L2 只钉「走 WebGPU 路径则用 WGSL」）。

### 6. 错误与轮询（Device）

来源：[wgpu `Device::poll`](https://docs.rs/wgpu/latest/wgpu/struct.Device.html)、WebGPU 设备丢失模型（[W3C WebGPU](https://www.w3.org/TR/webgpu/)）

- 映射回调 / 资源回收需按后端要求 **poll**（原生常需显式 poll；WebGPU 后端上 `Poll` 可为 no-op，`Wait` 可能不受支持——以当前 rustdoc 为准）。
- 设备丢失、校验错误须可观测（log / 回调 / 返回错误）；禁止假定「submit 永远成功」且无降级路径（具体回调形状 → L3）。

## 判据速查

| 场景 | 结论 |
|---|---|
| 每帧 `request_device` / 狂建 Device | **FAIL** → 启动期各一份 Device/Queue |
| 仍持有旧 `SurfaceTexture` 时 `configure` | **FAIL** |
| 每帧数十次 `submit` 或每帧新建大量 buffer | **FAIL**（Dos and Don’ts） |
| 更新 uniform 默认走临时 map 而非 `write_buffer` | **FAIL**（除非有测过的 staging 池理由） |
| WebGPU 路径着色器用 GLSL 字符串直喂 wgpu | **FAIL** → WGSL（或 L3 批准的转译产物） |
| 静默改用 GLES 冒充「已通 wgpu」 | **各项目 L3**（非本文件） |
| Rust 命名 / `unsafe` / rustfmt | **L1** [`lang_rust.md`](lang_rust.md) |

## 非本文件

- Rust 语言与 API Guidelines → L1 [`lang_rust.md`](lang_rust.md)
- Android / iOS / 鸿蒙窗口线程与 Activity 生命周期 → 各 OS 的 L2（`platform_android` 等）
- Holm Runtime 旁路、BLOCKED 政策、包体门禁 → **各项目 L3**
