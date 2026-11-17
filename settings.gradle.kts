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

include(":holm-core")
include(":holm-renderer-gl")
include(":holm-renderer-wgpu")
include(":KuiklyHolmCore")
include(":androidApp")
include(":holmDemo")
include(":h5App")
