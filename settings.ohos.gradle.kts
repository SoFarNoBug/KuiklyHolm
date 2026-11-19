pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
    }
}

rootProject.name = "KuiklyHolmUI"

val buildFileName = "build.ohos.gradle.kts"
rootProject.buildFileName = buildFileName

// ADR 0015：local → includeBuild ../kmp-wgpu/ohosInclude（KBA + ohosArm64；勿 include 主 settings）
val localPropsFile = file("local.properties")
val localProps = java.util.Properties()
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { localProps.load(it) }
}
val useLocalKmpWgpu =
    providers.gradleProperty("kmp.wgpu.local").orNull.equals("true", ignoreCase = true) ||
        localProps.getProperty("kmp.wgpu.local")?.equals("true", ignoreCase = true) == true

if (useLocalKmpWgpu) {
    val kmpWgpuOhosDir = file("../kmp-wgpu/ohosInclude")
    if (!kmpWgpuOhosDir.isDirectory) {
        throw GradleException(
            "kmp.wgpu.local=true but missing ${kmpWgpuOhosDir.absolutePath} (ADR 0015 Ohos)",
        )
    }
    includeBuild(kmpWgpuOhosDir) {
        name = "kmp-wgpu-ohos"
        dependencySubstitution {
            substitute(module("io.github.sofarnobug:kmp-wgpu")).using(project(":kmp-wgpu"))
            substitute(module("io.github.sofarnobug:kmp-wgpu-surface")).using(project(":kmp-wgpu-surface"))
        }
    }
}

// ADR 0017 / 0019：kmp-draco Ohos；旁路仓存在则默认 includeBuild（与主 settings 同口径）
val kmpDracoDir = file("../kmp-draco")
val kmpDracoLocalProp =
    providers.gradleProperty("kmp.draco.local").orNull
        ?: localProps.getProperty("kmp.draco.local")
val useLocalKmpDraco = when {
    kmpDracoLocalProp.equals("false", ignoreCase = true) -> false
    kmpDracoLocalProp.equals("true", ignoreCase = true) -> true
    else -> kmpDracoDir.isDirectory
}
if (useLocalKmpDraco) {
    val kmpDracoOhosDir = file("../kmp-draco/ohosInclude")
    if (!kmpDracoOhosDir.isDirectory) {
        throw GradleException(
            "kmp.draco.local=true but missing ${kmpDracoOhosDir.absolutePath} (ADR 0019 Ohos)",
        )
    }
    includeBuild(kmpDracoOhosDir) {
        name = "kmp-draco-ohos"
        dependencySubstitution {
            substitute(module("io.github.sofarnobug:kmp-draco")).using(project(":kmp-draco"))
        }
    }
}

include(":holm-core")
project(":holm-core").buildFileName = "build.ohos.gradle.kts"
include(":holm-renderer-gl")
project(":holm-renderer-gl").buildFileName = "build.ohos.gradle.kts"
include(":holm-renderer-wgpu")
project(":holm-renderer-wgpu").buildFileName = "build.ohos.gradle.kts"
include(":holm-gltf")
project(":holm-gltf").buildFileName = "build.ohos.gradle.kts"
include(":KuiklyHolmCore")
project(":KuiklyHolmCore").buildFileName = buildFileName
include(":holmDemo")
project(":holmDemo").buildFileName = buildFileName
