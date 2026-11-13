pluginManagement {
    repositories {
        // 腾讯云：Maven 聚合 + Gradle Plugin Portal 镜像（无 google/central 直连）
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
include(":KuiklyHolmCore")
include(":androidApp")
include(":holmDemo")
include(":h5App")
// miniApp deferred (grill Q4)
