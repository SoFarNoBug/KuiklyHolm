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

include(":holm-core")
project(":holm-core").buildFileName = "build.ohos.gradle.kts"
include(":kmp-wgpu")
project(":kmp-wgpu").buildFileName = "build.ohos.gradle.kts"
include(":kmp-wgpu-surface")
project(":kmp-wgpu-surface").buildFileName = "build.ohos.gradle.kts"
include(":holm-renderer-gl")
project(":holm-renderer-gl").buildFileName = "build.ohos.gradle.kts"
include(":holm-renderer-wgpu")
project(":holm-renderer-wgpu").buildFileName = "build.ohos.gradle.kts"
include(":KuiklyHolmCore")
project(":KuiklyHolmCore").buildFileName = buildFileName
include(":holmDemo")
project(":holmDemo").buildFileName = buildFileName
