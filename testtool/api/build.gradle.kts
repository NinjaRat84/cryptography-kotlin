/*
 * Copyright (c) 2023-2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    alias(kotlinLibs.plugins.multiplatform)
    alias(kotlinLibs.plugins.serialization)
}

kotlin {
    explicitApi()

    jvmToolchain(8)

    jvm()
    js(IR) {
        nodejs()
        browser()
    }

    iosArm64()
    iosX64()
    iosSimulatorArm64()

    watchosX64()
    watchosArm32()
    watchosArm64()
    watchosSimulatorArm64()

    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()

    macosX64()
    macosArm64()

    linuxX64()
    linuxArm64()
    mingwX64()

    // No support from KTOR
    watchosDeviceArm64()
    androidNativeX64()
    androidNativeX86()
    androidNativeArm64()
    androidNativeArm32()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.serialization.core)

                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.serialization.protobuf)
            }
        }
    }
}
