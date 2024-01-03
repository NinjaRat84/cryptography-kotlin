/*
 * Copyright (c) 2023-2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.cryptography.testtool.server.db

import kotlinx.datetime.*
import org.jetbrains.exposed.dao.id.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.*
import java.util.*
import kotlin.sequences.Sequence

internal object Actors : UUIDTable("actors") {
    val instanceId = text("instance_id")
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }

    val platform = text("platform")
    val provider = text("provider")

    fun save(
        instanceId: String,
        platform: String,
        provider: String,
    ): EntityID<UUID> = insertAndGetId {
        it[this.instanceId] = instanceId
        it[this.platform] = platform
        it[this.provider] = provider
    }

    fun getAll(): Sequence<ResultRow> = sequence {
        var offset = 0L
        do {
            val result = selectAll().limit(DB_BATCH_SIZE, offset).toList()
            yieldAll(result)
            offset += result.size
        } while (result.size == DB_BATCH_SIZE)
    }
}
