/*
 * Copyright (c) 2023-2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.kotlin.gradle.dsl.*

plugins {
    alias(kotlinLibs.plugins.jvm)
}

kotlin {
    explicitApi()

    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_1_8)
        apiVersion.set(KotlinVersion.KOTLIN_1_8)
    }
}

dependencies {
    implementation(projects.api)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.calllogging)
    implementation(libs.ktor.server.cors)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.datetime)

    implementation(libs.sqlite)
//    implementation(libs.logback.classic)
}
