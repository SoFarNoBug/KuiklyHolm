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

    ohosArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("io.github.sofarnobug:kmp-draco:0.1.0")
            }
        }
        val ohosArm64Main by getting {
            dependencies {
                // ADR 0019：KHR_draco → kmp-draco（ohosInclude / Maven）
                implementation("io.github.sofarnobug:kmp-draco:0.1.0")
            }
        }
    }
}

android {
    namespace = "com.jlj.kuiklybase.holm.gltf"
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
