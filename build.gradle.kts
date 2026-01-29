@file:OptIn(ExperimentalWasmDsl::class)
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractNativeLibrary
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    id("maven-publish")
    id("org.jetbrains.dokka") version "2.1.0"
    id("com.vanniktech.maven.publish") version "0.35.0"
    id("com.github.ben-manes.versions") version "0.53.0"
}

group = "io.github.dreammooncai"
version = "1.5.1"

val dreamLib: KotlinNativeTarget.() -> Unit = {

    binaries {
        fun AbstractNativeLibrary.lib() {
            baseName = "KDreamEverythingLib"
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
    androidLibrary {
        namespace = "io.github.dreammooncai.everything"
        compileSdk = 36
        minSdk = 26
        compilerOptions.jvmTarget.set(JvmTarget.valueOf("JVM_${JavaVersion.current().majorVersion}"))
    }
    macosX64(dreamLib)
    macosArm64(dreamLib)
    iosX64(dreamLib)
    iosArm64(dreamLib)
    iosSimulatorArm64(dreamLib)
    mingwX64("windows", dreamLib)
    linuxX64(dreamLib)
    linuxArm64(dreamLib)
    androidNativeArm64(dreamLib)
    androidNativeArm32(dreamLib)
    androidNativeX86(dreamLib)
    androidNativeX64(dreamLib)
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
            implementation(libs.kotlinx.coroutines)
            implementation(kotlin("reflect"))
            implementation(libs.io.ktor.server.core)
            implementation(libs.io.ktor.server.websockets)
            implementation(libs.kotlinx.serialization)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        val jvmMain by creating {
            dependsOn(commonMain.get())
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
            languageSettings.enableLanguageFeature("ContextParameters")
            compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
            compilerOptions.freeCompilerArgs.add("-XXLanguage:+CustomEqualsInValueClasses")
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group as String, name, version as String)

    pom {
        name = "NDArray.simd"
        description = "Kotlin/Multiplatform NDArray with SIMD optimizations and low memory footprint"
        url.set("https://github.com/DreamMoonCai/multiplatform-everything/")
        licenses {
            license {
                name = "3-Clause BSD NON-AI License"
                url = "https://github.com/non-ai-licenses/non-ai-licenses/blob/main/NON-AI-BSD3"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id.set("DreamMoonCai")
                name.set("dreammoon")
                url.set("https://github.com/DreamMoonCai/")
            }
        }
        scm {
            url.set("https://github.com/DreamMoonCai/multiplatform-everything")
            connection.set("scm:git:git://github.com/DreamMoonCai/multiplatform-everything.git")
            developerConnection.set("scm:git:ssh://git@github.com/DreamMoonCai/multiplatform-everything.git")
        }
    }
}

publishing {
    repositories {
        maven {
            name = "localPluginRepository"
            url = uri("../local-plugin-repository")
        }
    }
}
