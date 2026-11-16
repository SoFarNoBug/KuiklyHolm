import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    kotlin("plugin.compose")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

val mavenVersion: String = findProperty("mavenVersion") as? String
    ?: findProperty("MAVEN_VERSION") as? String
    ?: "1.0.0-2.31.0"
val groupId: String = findProperty("groupId") as? String
    ?: findProperty("GROUP_ID") as? String
    ?: "io.github.sofarnobug"

group = groupId
version = mavenVersion

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
        publishLibraryVariants("release")
    }

    js(IR) {
        browser()
        binaries.executable()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // 公开签名暴露 Kuikly Compose 类型（Modifier/Color 等），必须用 api 传递，消费方才能解析。
                api("com.tencent.kuikly-open:core:${Version.getKuiklyVersion()}")
                api("com.tencent.kuikly-open:core-annotations:${Version.getKuiklyVersion()}")
                api("com.tencent.kuikly-open:compose:${Version.getKuiklyVersion()}")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            }
        }

        val androidMain by getting {
            dependencies {
                compileOnly("com.tencent.kuikly-open:core-render-android:${Version.getKuiklyVersion()}")
                implementation(project(":kmp-wgpu"))
                implementation(project(":kmp-wgpu-surface"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("com.tencent.kuikly-open.core-render-web:base:${Version.getKuiklyVersion()}")
                implementation(project(":kmp-wgpu"))
                implementation(project(":kmp-wgpu-surface"))
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

android {
    namespace = "com.jlj.kuiklybase.holm"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }
    buildToolsVersion = "36.0.0"
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

// 可选：显式传入 mavenRepoUrl 时启用 GitHub Packages（不影响 Central）
publishing {
    repositories {
        val gpUrl = findProperty("mavenRepoUrl") as? String
        if (!gpUrl.isNullOrBlank()) {
            maven {
                url = uri(gpUrl)
                credentials {
                    username = findProperty("mavenUsername") as? String ?: ""
                    password = findProperty("mavenPassword") as? String ?: ""
                }
            }
        }
    }
}

// ---- Maven Central（vanniktech：坐标 / POM / 签名 / 上传）----
mavenPublishing {
    coordinates("io.github.sofarnobug", "KuiklyHolmCore", project.version.toString())
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    if (System.getenv("SKIP_SIGN") != "1") signAllPublications()

    pom {
        name.set("KuiklyHolmCore")
        description.set("Holm Kuikly local GPU effects core (KMP shell)")
        url.set("https://github.com/SoFarNoBug/KuiklyHolm")
        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("sofarnobug")
                name.set("SoFarNoBug")
            }
        }
        scm {
            url.set("https://github.com/SoFarNoBug/KuiklyHolm")
            connection.set("scm:git:git://github.com/SoFarNoBug/KuiklyHolm.git")
            developerConnection.set("scm:git:ssh://git@github.com/SoFarNoBug/KuiklyHolm.git")
        }
    }
}
