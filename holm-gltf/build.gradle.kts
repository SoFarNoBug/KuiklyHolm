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

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    js(IR) {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
            }
        }
        val androidMain by getting {
            dependencies {
                // ADR 0017：KHR_draco → kmp-draco（local includeBuild 或 Maven 0.1.0）
                implementation("io.github.sofarnobug:kmp-draco:0.1.0")
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
            dependencies {
                // ADR 0017 / iOS Model：与 androidMain 同坐标
                implementation("io.github.sofarnobug:kmp-draco:0.1.0")
            }
        }
        val jsMain by getting {
            dependencies {
                // ADR 0020：H5 Draco → window.__kmpDraco
                implementation("io.github.sofarnobug:kmp-draco:0.1.0")
            }
        }
    }
}

android {
    namespace = "com.jlj.kuiklybase.holm.gltf"
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
