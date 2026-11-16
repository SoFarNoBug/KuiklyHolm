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
                api(project(":holm-core"))
            }
        }

        val androidMain by getting {
            dependencies {
                compileOnly("com.tencent.kuikly-open:core-render-android:${Version.getKuiklyVersion()}")
                api(project(":holm-renderer-gl"))
                api(project(":holm-renderer-wgpu"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("com.tencent.kuikly-open.core-render-web:base:${Version.getKuiklyVersion()}")
                api(project(":holm-renderer-gl"))
                api(project(":holm-renderer-wgpu"))
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                api(project(":holm-renderer-gl"))
                api(project(":holm-renderer-wgpu"))
            }
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

mavenPublishing {
    coordinates("io.github.sofarnobug", "KuiklyHolmCore", project.version.toString())
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    if (System.getenv("SKIP_SIGN") != "1") signAllPublications()

    pom {
        name.set("KuiklyHolmCore")
        description.set("Holm Kuikly local GPU effects umbrella (holm-core + renderers)")
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
