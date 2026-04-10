import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    id("distribution")
}

group = "com.mckli"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass.set("com.mckli.MainKt")
        }
        testRuns.named("test") {
            executionTask.configure {
                useJUnitPlatform()
                testLogging {
                    showStandardStreams = true
                }
            }
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
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        hostOs == "Linux" && isArm64 -> null // linuxArm64("native")
        hostOs.startsWith("Windows") -> mingwX64("native")
        else -> null
    }

    nativeTarget?.apply {
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
                implementation("io.github.oshai:kotlin-logging:7.0.0")
                implementation("com.github.ajalt.clikt:clikt:5.1.0")
                implementation("io.ktor:ktor-client-core:2.3.12")
                implementation("io.ktor:ktor-client-cio:2.3.12")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
                implementation("ch.qos.logback:logback-classic:1.5.6")
                implementation("io.ktor:ktor-server-core:2.3.12")
                implementation("io.ktor:ktor-server-netty:2.3.12")
                implementation("io.ktor:ktor-server-content-negotiation:2.3.12")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
                implementation("io.cucumber:cucumber-java8:7.18.1")
                implementation("io.cucumber:cucumber-junit-platform-engine:7.18.1")
                implementation("org.junit.platform:junit-platform-suite:1.10.3")
                implementation("io.mockk:mockk:1.13.12")
                implementation("io.ktor:ktor-server-core:2.3.12")
                implementation("io.ktor:ktor-server-netty:2.3.12")
                implementation("io.ktor:ktor-server-content-negotiation:2.3.12")
                implementation("ch.qos.logback:logback-classic:1.5.6")
            }
        }

        if (nativeTarget != null) {
            val nativeMain by getting
            val nativeTest by getting
        }
    }
}

tasks.named<Jar>("jvmJar") {
    manifest {
        attributes["Main-Class"] = "com.mckli.MainKt"
    }
}

val startScripts = tasks.register<CreateStartScripts>("startScripts") {
    mainClass.set("com.mckli.MainKt")
    applicationName = "mckli"
    outputDir = layout.buildDirectory.dir("scripts").map { it.asFile }.get()
    defaultJvmOpts = listOf("--enable-native-access=ALL-UNNAMED")
    val jvmJar = tasks.named<Jar>("jvmJar").get()
    val runtimeClasspath = configurations.named("jvmRuntimeClasspath").get()
    classpath = files(jvmJar.archiveFile) + runtimeClasspath
}

configure<DistributionContainer> {
    named("main") {
        contents {
            from(startScripts) {
                into("bin")
            }
            from(tasks.named("jvmJar")) {
                into("lib")
            }
            from(configurations.named("jvmRuntimeClasspath")) {
                into("lib")
            }
        }
    }
}
