/*
 * Copyright (c) 2023 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
//        mavenLocal {
//            content {
//                includeGroup("dev.whyoleg.cryptography")
//            }
//        }
        mavenCentral()
    }

    versionCatalogs {
        create("cryptographyLibs") {
            from("dev.whyoleg.cryptography:cryptography-version-catalog:53")
        }
    }
}

rootProject.name = "all-targets-setup"
