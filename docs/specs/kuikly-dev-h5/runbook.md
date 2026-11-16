# runbook · kuikly-dev-h5

> 浏览器冒烟 / 双端口起服 / 白屏与构建排障。维护者入口亦见根 [CONTRIBUTING.md](../../../CONTRIBUTING.md)。

## 结论

本仓 H5 可冒烟：**双进程 + 双端口**；业务包走精简 Node 静态服（**不用** Kuikly 官方 whistle / npm serve）。  
已验证（本机）：`8080` / `8083` 均 200；入口 `?page_name=home` → **示例1 / 双轨** / `holm_runtime_m0`。

## 拓扑

```text
浏览器
  └─ http://localhost:8080/          ← h5App webpack-dev-server（宿主：h5App.js + index.html + assets）
         │  <script> 拉取
         └─ http://127.0.0.1:8083/nativevue2.js
                                       ← static_server/serve（业务 KMP 包，CORS *）
```

| 端口 | 产物 | 谁生成 / 谁托管 |
|---|---|---|
| **8080** | `h5App.js`、`index.html`、样例图 | `:h5App:jsBrowserDevelopmentRun` + `copyAssetsToWebpackDevServer` |
| **8083** | `nativevue2.js`（约 10+ MB） | `:holmDemo:packLocalJsBundleDebug` + `node static_server/serve/index.js` |

`index.html` 脚本顺序必须是：**先** `nativevue2.js`，**再** `h5App.js`。中间不要插会挂起的同步 CDN。

## 前置

| 项 | 本机约定 |
|---|---|
| JDK | **17**（例：`D:\tool\jdk-17`；勿用 25） |
| node | 在 PATH（例：Cursor helpers `d:\tool\cursor\resources\app\resources\helpers`） |
| Yarn 缓存 | 若 pack 因 GitHub 超时：先 `node static_server/fetch-yarn-ivy.js` |

PowerShell 示例：

```powershell
$env:JAVA_HOME = "D:\tool\jdk-17"
$nodeDir = "d:\tool\cursor\resources\app\resources\helpers"
$env:Path = "$env:JAVA_HOME\bin;$nodeDir;" + $env:Path
cd D:\code_space\kuikly-holm
```

## 标准步骤

```bash
# 若 pack 因 GitHub 拉 Yarn 超时（一次即可）
node static_server/fetch-yarn-ivy.js

# 终端 1：业务包 + 8083
./gradlew :holmDemo:packLocalJsBundleDebug
node static_server/serve/index.js

# 终端 2：宿主
./gradlew :h5App:copyAssetsToWebpackDevServer
./gradlew :h5App:jsBrowserDevelopmentRun
```

打开：`http://localhost:8080/?page_name=home` → Holm 入口。

| pageName | 角色 |
|---|---|
| `home` | 入口 |
| `holm_runtime_m0` | 示例1（隧道）/ Lite↔Runtime 双轨 |

| 改了什么 | 重跑什么 |
|---|---|
| Demo / Core common 或 js 逻辑 | `packLocalJsBundleDebug`，并**重启** 8083（或确认 serve 读到新文件） |
| `h5App` 宿主 / `webpack.config.d` | 重启 `jsBrowserDevelopmentRun`（config.d **不会**热更新） |
| `src/.../assets` 样例图 | `copyAssetsToWebpackDevServer` |
| 仅 `index.html` | 一般 `jsProcessResources` 后刷新即可；不保险时重启 webpack |

仅编译宿主（不起服）：

```bash
./gradlew :h5App:jsBrowserDevelopmentWebpack
```

## 工程约定（务必保留）

1. **`h5App/webpack.config.d/output.js`（方案 X）**  
   把 `h5App.js` 打成 **IIFE**，禁止 UMD 整 key 覆盖 `window.com`。  
   多模块下 `nativevue2` 挂的是 `com.tencent.kuikly.core.nvi.*`，`h5App` 挂的是 `…render.web.*`；UMD 后加载会整块抹掉 `nvi` → `registerCallNative` 失败 → 白屏。  
   官方说明见 Kuikly `h5App/README.md`「多模块工程下 UMD 全局命名空间被覆盖」。备选方案 Y（深合并）本仓未启用；**勿与 X 同时开**。

2. **`static_server/serve`**  
   零 npm 依赖的纯 `http` 服；按候选目录找够大的 `nativevue2.js`；对 JS 回 `Access-Control-Allow-Origin: *`。不要为冒烟再引回 whistle。  
   可选：`H5_BUNDLE_PORT`、`H5_BUNDLE_DIR`。

3. **`index.html` 不要默认同步挂 libpag CDN**  
   CDN 超时会卡住后续 `h5App.js`（白屏）。需要 PAG 时再加，并保证可访问或改本地/async。

4. **Yarn / Node 下载**  
   根 `build.gradle.kts` 指向 `%USERPROFILE%\.gradle\caches\manual-yarn\ivy`；GitHub 不通时先跑 `fetch-yarn-ivy.js`（腾讯镜像）。

5. **Windows + 未装 CocoaPods**  
   `gradle.properties` 可设 `kotlin.native.ignoreDisabledTargets=true`，避免无关 native 目标拖死配置。

## Holm 示例1 目视（holm_runtime_m0）

- 示例1：霓虹隧道（Lite GLSL / Runtime WGSL）  
- `maxFps`：`0 / 30 / 60 / 120`
- `renderScale`：默认 `1`；backing = CSS×scale（仍不乘 DPR）；与 `maxFps` 正交
- **H5 限帧语义**：`onPerf` = 真实 WebGL 提交率；`maxFps` 经 rAF + `nextDue` **跳提交**（与 Android `setFrameRate` 提示不同）。见 `HolmGlPumpSource`。
- backend：`onReady` 报 `webgl` / `webgl-worker`（Lite）或 `wgpu-m0`（Runtime）

## 与 Android 对照

| | H5 | Android |
|---|---|---|
| 命令 | 本 runbook 双端口 | 见 [kuikly-dev-android/runbook.md](../kuikly-dev-android/runbook.md) |
| 后端标签 | `webgl` / `webgl-worker` | `gles2` |
| maxFps | rAF + nextDue 跳提交 | setFrameRate 提示；CONTINUOUSLY 下 30 仍可能 ~60 |

## 白屏 / 构建排障（本仓踩过）

| 现象 | 先查 | 处理 |
|---|---|---|
| `#root` 空、无 UI | Network：`libpag` / 外链是否 Pending | 去掉或本地化阻塞脚本；保证 `h5App.js` 能执行 |
| Console：`registerCallNative` / `nvi` undefined | `h5App.js` 是否仍为 UMD（头尾含 `webpackUniversalModuleDefinition`） | 确认存在 `webpack.config.d/output.js` 并**重启** webpack |
| 8080 200 但仍空 | 8083 的 `nativevue2.js` 是否 200、体积是否数 MB | 重跑 pack；看 serve 启动日志解析到的目录 |
| pack 卡在 Yarn | GitHub 超时 | `node static_server/fetch-yarn-ivy.js` 后再 pack |
| 改了 config.d 无效 | 旧 webpack 进程仍在 | 杀 8080 占用进程后重跑 `jsBrowserDevelopmentRun` |
| `Cannot expand ZIP … kotlin_resources.zip` | 缓存里的 zip 实为反爬 HTML，或真损坏 | 见下节；**勿只删了再 `--refresh-dependencies`**（会再下到 HTML） |
| Runtime 岛黑屏 / 深蓝清屏，Lite 正常 | 坏 WGSL 编译失败未接 onError（旧 wasm）或业务未看错误条 | 先看页内 `onError SHADER_CUSTOM` / `WGPU_*`；改 `holm-runtime` 后须重跑 `holm-runtime/build-wasm.ps1` 再 pack H5 |

### compose-js `kotlin_resources.zip`（pack / webpack 失败）

`:holmDemo:packLocalJsBundleDebug` / `:h5App:jsBrowserDevelopmentRun` 等在
`:…:jsResolveResourcesFromDependencies` 报：

`Cannot expand ZIP '…\compose-js\2.31.0-2.1.21\…\compose-2.31.0-2.1.21.kotlin_resources.zip'`

#### 根因（本仓踩过，2026-11-15）

| 点 | 说明 |
|---|---|
| 合法包很小 | `compose-js` 的 `jsResourcesElements` 元数据里该 zip **约 350 B**（空资源壳），**不能**单凭「几百字节」判坏 |
| 坏包特征 | 体积常 ~1 KB；用文本打开开头是 `<script>…`（腾讯云反爬 / Bot 挑战），**不是** `PK` zip 头 |
| 易踩镜像 | Gradle 经 `mirrors.cloud.tencent.com/.../maven-public/` 拉该文件时，有时落到 HTML；`--refresh-dependencies` 仍会再下坏包 |
| 可用源 | `https://mirrors.tencent.com/nexus/repository/maven-tencent/com/tencent/kuikly-open/compose-js/<ver>/compose-js-<ver>-kotlin_resources.kotlin_resources.zip` |

版本号以工程依赖为准（例：`2.31.0-2.1.21`）。校验值以 Gradle 缓存里对应 `.module` 的 `jsResourcesElements-published.files[]` 为准（同版示例：`sha1=ec1367d3…`、`sha256=f288c648…`、`size=350`）。

#### 处理

1. 确认坏包：打开报错路径的 zip，若是 HTML/`<script>` → 按下面手工灌缓存；若是真 zip 损坏再谈删缓存重下。  
2. `./gradlew --stop`  
3. 删掉所有坏副本：  
   `%USERPROFILE%\.gradle\caches\modules-2\files-2.1\com.tencent.kuikly-open\compose-js\<ver>\**\*.kotlin_resources.zip`  
4. 从 **maven-tencent** 下载正确文件，校验 sha1/sha256 与 `.module` 一致后，装到：  
   `…\compose-js\<ver>\<sha1>\compose-2.31.0-2.1.21.kotlin_resources.zip`  
   （缓存内文件名是 `compose-<ver>.kotlin_resources.zip`；远端 URL 名是 `compose-js-<ver>-kotlin_resources.kotlin_resources.zip`。）  
5. **不要**立刻 `--refresh-dependencies` 覆盖刚灌好的包；直接重跑 `packLocalJsBundleDebug` / `jsBrowserDevelopmentRun`。

PowerShell 示例（版本按实际改）：

```powershell
$ver = "2.31.0-2.1.21"
$url = "https://mirrors.tencent.com/nexus/repository/maven-tencent/com/tencent/kuikly-open/compose-js/$ver/compose-js-$ver-kotlin_resources.kotlin_resources.zip"
$out = "$env:TEMP\compose-kotlin_resources.zip"
Invoke-WebRequest -Uri $url -OutFile $out -UseBasicParsing
$sha1 = (Get-FileHash $out -Algorithm SHA1).Hash.ToLower()
# 对照 .module 的 sha1 / size（合法约 350）
Get-ChildItem "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\com.tencent.kuikly-open\compose-js\$ver" -Recurse -Filter "*.kotlin_resources.zip" | Remove-Item -Force
$dest = "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\com.tencent.kuikly-open\compose-js\$ver\$sha1"
New-Item -ItemType Directory -Force -Path $dest | Out-Null
Copy-Item $out "$dest\compose-$ver.kotlin_resources.zip" -Force
.\gradlew.bat --stop
.\gradlew.bat :holmDemo:packLocalJsBundleDebug
```

若反复从 `maven-public` 拉到 HTML，优先保证 `settings.gradle.kts` 里 **maven-tencent** 可解析到该坐标；手工灌缓存只是本机应急。

快速自检（PowerShell 思路）：

- `http://127.0.0.1:8080/`、`/h5App.js`、`http://127.0.0.1:8083/nativevue2.js` 均 200  
- `h5App.js` **不含** `webpackUniversalModuleDefinition`  
- 浏览器：`#root` 下出现业务节点；可进 `holm_runtime_m0`

## 相关路径

| 路径 | 说明 |
|---|---|
| `h5App/` | 宿主壳；`webpack.config.d/output.js` |
| `static_server/serve/index.js` | 8083 业务包静态服 |
| `KuiklyHolmCore/src/jsMain/.../HolmGlPumpSource.kt` | WebGL 泵 / maxFps |
| `holmDemo/.../HolmRuntimeM0Pager.kt` | `holm_runtime_m0`（示例1） |
| `docs/specs/holm-render-substrate/changelog.md` | 底座 / 删五席 changelog |
