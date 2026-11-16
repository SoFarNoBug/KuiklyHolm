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

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":holm-core"))
            }
        }
        val androidMain by getting {
            dependencies {
                api(project(":kmp-wgpu"))
                api(project(":kmp-wgpu-surface"))
            }
        }
        val jsMain by getting {
            dependencies {
                api(project(":kmp-wgpu"))
                api(project(":kmp-wgpu-surface"))
                implementation("com.tencent.kuikly-open.core-render-web:base:${Version.getKuiklyVersion()}")
            }
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
