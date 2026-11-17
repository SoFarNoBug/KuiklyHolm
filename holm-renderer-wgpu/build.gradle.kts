plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    js(IR) {
        browser()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":holm-core"))
            }
        }
        val androidMain by getting {
            dependencies {
                api("io.github.sofarnobug:kmp-wgpu:0.1.0")
                api("io.github.sofarnobug:kmp-wgpu-surface:0.1.0")
            }
        }
        val jsMain by getting {
            dependencies {
                api("io.github.sofarnobug:kmp-wgpu:0.1.0")
                api("io.github.sofarnobug:kmp-wgpu-surface:0.1.0")
                implementation("com.tencent.kuikly-open.core-render-web:base:${Version.getKuiklyVersion()}")
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                api("io.github.sofarnobug:kmp-wgpu:0.1.0")
                api("io.github.sofarnobug:kmp-wgpu-surface:0.1.0")
            }
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

android {
    namespace = "com.jlj.kuiklybase.holm.renderer.wgpu"
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
