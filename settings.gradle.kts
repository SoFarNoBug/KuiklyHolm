pluginManagement {
    repositories {
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        }
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugins/")
        }
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        }
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
        mavenLocal()
    }
}

rootProject.name = "KuiklyHolmUI"

// ADR 0015：默认 Maven；local.properties / -Pkmp.wgpu.local=true → includeBuild 旁路仓
val localPropsFile = file("local.properties")
val localProps = java.util.Properties()
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { localProps.load(it) }
}
val useLocalKmpWgpu =
    providers.gradleProperty("kmp.wgpu.local").orNull.equals("true", ignoreCase = true) ||
        localProps.getProperty("kmp.wgpu.local")?.equals("true", ignoreCase = true) == true

if (useLocalKmpWgpu) {
    val kmpWgpuDir = file("../kmp-wgpu")
    if (!kmpWgpuDir.isDirectory) {
        throw GradleException(
            "kmp.wgpu.local=true but missing ${kmpWgpuDir.absolutePath} (ADR 0015)",
        )
    }
    includeBuild(kmpWgpuDir) {
        dependencySubstitution {
            substitute(module("io.github.sofarnobug:kmp-wgpu")).using(project(":kmp-wgpu"))
            substitute(module("io.github.sofarnobug:kmp-wgpu-surface")).using(project(":kmp-wgpu-surface"))
        }
    }
}

// ADR 0017：kmp-draco 未发 Central 前，旁路仓存在则默认 includeBuild；
// 显式 kmp.draco.local=false 才走 Maven（mavenLocal / Central）。
val kmpDracoDir = file("../kmp-draco")
val dracoLocalProp =
    providers.gradleProperty("kmp.draco.local").orNull
        ?: localProps.getProperty("kmp.draco.local")
val useLocalKmpDraco = when {
    dracoLocalProp.equals("false", ignoreCase = true) -> false
    dracoLocalProp.equals("true", ignoreCase = true) -> true
    else -> kmpDracoDir.isDirectory
}

if (useLocalKmpDraco) {
    if (!kmpDracoDir.isDirectory) {
        throw GradleException(
            "kmp.draco.local=true but missing ${kmpDracoDir.absolutePath} (ADR 0017)",
        )
    }
    includeBuild(kmpDracoDir) {
        dependencySubstitution {
            substitute(module("io.github.sofarnobug:kmp-draco")).using(project(":kmp-draco"))
        }
    }
}

include(":holm-core")
include(":holm-renderer-gl")
include(":holm-renderer-wgpu")
include(":holm-gltf")
include(":KuiklyHolmCore")
include(":androidApp")
include(":holmDemo")
include(":h5App")
