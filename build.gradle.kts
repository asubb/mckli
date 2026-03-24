plugins {
    kotlin("multiplatform") version "2.3.20"
}

group = "com.mckli"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass.set("com.mckli.MainKt")
        }
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
                }
            }
        }
    }

    // Configure native targets - Clikt supports all major platforms
    // Note: Kotlin Native doesn't provide prebuilt compiler for Linux ARM64 host
    // So we skip native compilation on Linux ARM64, but it can cross-compile from other platforms
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"

    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs.startsWith("Windows") -> mingwX64("native")
        else -> null
    }
        ?: throw IllegalArgumentException("Host OS $hostOs (${System.getProperty("os.arch")}) is not supported in Kotlin/Native.")

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "com.mckli.main"
                baseName = "mckli"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.github.ajalt.clikt:clikt:5.1.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jvmMain by getting
        val jvmTest by getting

        if (nativeTarget != null) {
            val nativeMain by getting
            val nativeTest by getting
        }
    }
}
