plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("maven-publish")
    id("org.jetbrains.compose")
    kotlin("plugin.compose")
}

val KEY_PAGE_NAME = "pageName"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
        publishLibraryVariants("release")
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Perspective crop demo shared pages"
        homepage = "Link to the KuiklyHolm demo"
        version = "1.0"
        ios.deploymentTarget = "14.1"
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "holmDemo"
            freeCompilerArgs = freeCompilerArgs + getCommonCompilerArgs()
            isStatic = true
            license = "MIT"
        }
        extraSpecAttributes["resources"] = "['src/commonMain/assets/**']"
    }

    ohosArm64 {
        binaries.sharedLib("shared") {
            freeCompilerArgs += "-Xadd-light-debug=enable"
            linkerOpts += "--build-id=sha1"
            // MagnifierSampler.ohos → OH Image NDK
            linkerOpts += "-limage_source_ndk.z"
            linkerOpts += "-lpixelmap_ndk.z"
            linkerOpts += "-lz"
            linkerOpts += "-lnative_drawing"
            // kmp-wgpu Present → libkmp_wgpu_native.so（与 libshared 同目录装机；$ORIGIN）
            val holmRuntimeLibDir = rootProject.projectDir.resolve("ohosApp/entry/libs/arm64-v8a").absolutePath
            linkerOpts += "-L$holmRuntimeLibDir"
            linkerOpts += "-lkmp_wgpu_native"
            // lld (KN) 直接吃 -rpath，勿包 -Wl
            linkerOpts += "-rpath"
            linkerOpts += "\$ORIGIN"
            // keep @CName exports for NAPI / holm_xc dlsym (ELF, no underscore)
            linkerOpts += "-u"
            linkerOpts += "holm_set_resfile_dir"
            linkerOpts += "-u"
            linkerOpts += "holm_set_ohos_dirs"
            linkerOpts += "-u"
            linkerOpts += "holm_magnifier_blit_snapshot"
            linkerOpts += "-u"
            linkerOpts += "holm_ohos_bridge_require_provider"
            linkerOpts += "-u"
            linkerOpts += "holm_ohos_bridge_has_wgpu"
            linkerOpts += "-u"
            linkerOpts += "holm_ohos_bridge_create"
            linkerOpts += "-u"
            linkerOpts += "holm_ohos_bridge_destroy"
            linkerOpts += "-u"
            linkerOpts += "holm_ohos_bridge_tick"
            linkerOpts += "-u"
            linkerOpts += "holm_ohos_bridge_resize"
            linkerOpts += "-u"
            linkerOpts += "holm_ohos_bridge_set_custom_fs"
            linkerOpts += "-u"
            linkerOpts += "holm_ohos_bridge_set_scene"
            linkerOpts += "-u"
            linkerOpts += "holm_ohos_bridge_set_liquid"
            linkerOpts += "-u"
            linkerOpts += "holm_ohos_bridge_set_playing"
            linkerOpts += "-u"
            linkerOpts += "holm_ohos_bridge_backend_id"
            linkerOpts += "-u"
            linkerOpts += "holm_ohos_bridge_last_error_code"
            linkerOpts += "-u"
            linkerOpts += "holm_ohos_bridge_last_error_message"
            linkerOpts += "-u"
            linkerOpts += "holm_ohos_bridge_last_create_error"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":KuiklyHolmCore"))
                implementation("com.tencent.kuikly-open:core:${Version.getKuiklyOhosVersion()}")
                implementation("com.tencent.kuikly-open:core-annotations:${Version.getKuiklyOhosVersion()}")
                implementation("com.tencent.kuikly-open:compose:${Version.getKuiklyOhosVersion()}")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                api("com.tencent.kuikly-open:core-render-android:${Version.getKuiklyOhosVersion()}")
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
        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}

group = "com.jlj.kuiklyholmui"
version = System.getenv("kuiklyBizVersion") ?: "1.0.0"

publishing {
    repositories {
        maven {
            credentials {
                username = System.getenv("mavenUserName") ?: ""
                password = System.getenv("mavenPassword") ?: ""
            }
            rootProject.properties["mavenUr?"]?.toString()?.let { url = uri(it) }
        }
    }
}

ksp {
    arg(KEY_PAGE_NAME, getPageName())
}

dependencies {
    compileOnly("com.tencent.kuikly-open:core-ksp:${Version.getKuiklyOhosVersion()}") {
        add("kspAndroid", this)
        add("kspIosArm64", this)
        add("kspIosX64", this)
        add("kspIosSimulatorArm64", this)
        add("kspOhosArm64", this)
    }
}

android {
    // Ohos 壳走 AGP 7.4.2（build.ohos.gradle.kts）；勿用 AGP 8+ 的 compileSdk { release() }
    namespace = "com.jlj.kuiklyholmui.shared"
    compileSdk = 34
    buildToolsVersion = "34.0.0"
    defaultConfig {
        minSdk = 21
        targetSdk = 30
    }
    sourceSets {
        named("main") {
            assets.srcDirs("src/commonMain/assets")
        }
    }
}

fun getPageName(): String {
    return (project.properties[KEY_PAGE_NAME] as? String) ?: ""
}

fun getCommonCompilerArgs(): List<String> {
    return listOf(
        "-Xallocator=std"
    )
}
