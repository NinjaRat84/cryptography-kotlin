/*
 * Copyright (c) 2023-2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.cryptography.testtool.server

import dev.whyoleg.cryptography.testtool.server.db.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import java.io.*
import java.nio.file.*
import kotlin.io.path.*

// for running locally
internal fun main() {
    mergeTesttoolServerStorage("all", Path("build/testtool/server-storage"))

    return
    createTesttoolServer(
        instanceId = "local",
        storagePath = Path("build/testtool-local")
    ).start(wait = true)
}

// for use from plugin
public fun startTesttoolServer(
    instanceId: String,
    storagePath: Path,
): Closeable {
    println("TesttoolServer: starting...")
    val server = createTesttoolServer(instanceId, storagePath).start()
    println("TesttoolServer: started")

    return Closeable {
        println("TesttoolServer: stopping...")
        server.stop()
        println("TesttoolServer: stopped")
    }
}

public fun mergeTesttoolServerStorage(name: String, storagePath: Path) {
    mergeDatabases(storagePath, name)
}

private fun createTesttoolServer(
    instanceId: String,
    storagePath: Path,
): ApplicationEngine {
    val db = Database(mergeDatabases(storagePath, instanceId))

    val server = TesttoolServer(instanceId, db)

    return embeddedServer(Netty, 9000) {
        val handler = TesttoolRequestHandler(server, this)

        //TODO: redirect logback to file
        install(CallLogging) { disableDefaultColors() }
        install(CORS) { anyHost() }
        routing { post { handler.handle(call) } }
    }
}
