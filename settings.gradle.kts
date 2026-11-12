pluginManagement {
    repositories {
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
        google()
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

rootProject.name = "KuiklyHolmUI"
include(":KuiklyHolmCore")
include(":androidApp")
include(":holmDemo")
include(":h5App")
// miniApp deferred (grill Q4)
