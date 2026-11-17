plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }

    ohosArm64()

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
        val ohosArm64Main by getting {
            dependencies {
                api("io.github.sofarnobug:kmp-wgpu:0.1.0")
                api("io.github.sofarnobug:kmp-wgpu-surface:0.1.0")
            }
        }
    }
}

android {
    namespace = "com.jlj.kuiklybase.holm.renderer.wgpu"
    compileSdk = 34
    buildToolsVersion = "34.0.0"
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
