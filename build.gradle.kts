import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
    id("com.android.library") version "8.6.1"  // Updating causes a weird Gradle error?
    `maven-publish`
    id("org.jetbrains.dokka") version "2.0.0"
    id("com.vanniktech.maven.publish") version "0.31.0"
    id("com.github.ben-manes.versions") version "0.52.0"
}

group = "com.martmists.multiplatform-everything"
version = "1.4.1"

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}

val everythingLib: KotlinNativeTarget.() -> Unit = {

    binaries {
        fun NativeLib.lib() {
            baseName = "KEverythingLib"
        }
        if (konanTarget.family.isAppleFamily) framework {
            isStatic = true
            lib()
        }
        staticLib { lib() }
        sharedLib { lib() }
    }
}

kotlin {
    jvm("desktop")
    androidTarget()
    macosX64(everythingLib)
    macosArm64(everythingLib)
    iosX64(everythingLib)
    iosArm64(everythingLib)
    iosSimulatorArm64(everythingLib)
    mingwX64("windows", everythingLib)
    linuxX64(everythingLib)
    linuxArm64(everythingLib)
    androidNativeArm64(everythingLib)
    androidNativeArm32(everythingLib)
    androidNativeX86(everythingLib)
    androidNativeX64(everythingLib)
    js(IR) {
        nodejs()
        browser()
    }
    wasmJs {
        browser()
        nodejs()
        binaries.library()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(kotlin("stdlib"))
            implementation(kotlin("reflect"))
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("io.ktor:ktor-server-core:3.1.3")
            implementation("io.ktor:ktor-server-websockets:3.1.3")
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        val jvmMain by creating {
            dependsOn(commonMain.get())
        }

        val jvmTest by creating {
            dependsOn(commonTest.get())
        }

        val desktopMain by getting {
            dependsOn(jvmMain)
        }

        desktopMain.dependencies {

        }

        androidMain {
            dependsOn(jvmMain)
        }

        all {
            // compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes") if needed
        }
    }
}

android {
    compileSdk = 35
    namespace = "com.martmists.multiplatformeverything"
    ndkVersion = "21.4.7075529"

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


if (findProperty("mavenToken") != null) {
    fun MavenPom.configure() {
        name = "NDArray.simd"
        description = "Kotlin/Multiplatform NDArray with SIMD optimizations and low memory footprint"
        url = "https://github.com/martmists-gh/multiplatform-everything"

        licenses {
            license {
                name = "3-Clause BSD NON-AI License"
                url = "https://github.com/non-ai-licenses/non-ai-licenses/blob/main/NON-AI-BSD3"
                distribution = "repo"
            }
        }

        developers {
            developer {
                id = "Martmists"
                name = "Martmists"
                url = "https://github.com/martmists-gh"
            }
        }

        scm {
            url = "https://github.com/martmists-gh/multiplatform-everything"
        }
    }

    publishing {
        repositories {
            maven {
                name = "Releases"
                url = uri("https://maven.martmists.com/releases")
                credentials {
                    username = "admin"
                    password = project.ext["mavenToken"]!! as String
                }

            }
        }

        publications {
            withType<MavenPublication> {
                version = project.version as String
                pom {
                    configure()
                }
            }
        }
    }

    mavenPublishing {
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
        coordinates(group as String, name, version as String)
        signAllPublications()

        pom {
            configure()
        }
    }
}
