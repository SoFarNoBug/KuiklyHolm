plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    kotlin("plugin.compose")
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
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
        publishLibraryVariants("release")
    }

    ohosArm64 {
        val main by compilations.getting
        val ohosImage by main.cinterops.creating {
            defFile(project.file("src/ohosArm64Main/ohosInterop/cinterop/ohosImage.def"))
            includeDirs(project.file("src/ohosArm64Main/ohosInterop/include"))
        }
        val ohosDrawing by main.cinterops.creating {
            defFile(project.file("src/ohosArm64Main/ohosInterop/cinterop/ohosDrawing.def"))
            includeDirs(project.file("src/ohosArm64Main/ohosInterop/include"))
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":holm-core"))
            }
        }
        val androidMain by getting {
            dependencies {
                compileOnly("com.tencent.kuikly-open:core-render-android:${Version.getKuiklyOhosVersion()}")
            }
        }
        val ohosArm64Main by getting {
            dependencies {
                api(project(":holm-renderer-gl"))
                api(project(":holm-renderer-wgpu"))
            }
        }
    }
}

android {
    // Ohos 壳走 AGP 7.4.2；勿用 AGP 8+ compileSdk { release() }
    namespace = "com.jlj.kuiklybase.holm"
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
