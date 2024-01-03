/*
 * Copyright (c) 2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.cryptography.testtool.server.db

import kotlinx.datetime.*
import org.jetbrains.exposed.dao.id.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.*
import java.util.*
import kotlin.sequences.Sequence

internal object Datas : UUIDTable("datas") {
    val instanceId = text("instance_id")
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val createdBy = reference("created_by", Actors)

    val metadataId = reference("metadata_id", Metadatas)
    val content = text("content")

    fun save(
        instanceId: String,
        createdBy: String,
        metadataId: String,
        content: String,
    ): EntityID<UUID> = insertAndGetId {
        it[this.instanceId] = instanceId
        it[this.createdBy] = UUID.fromString(createdBy)
        it[this.metadataId] = UUID.fromString(metadataId)
        it[this.content] = content
    }

    fun getAll(metadataId: String): Sequence<ResultRow> = sequence {
        var offset = 0L
        do {
            val result = select {
                this@Datas.metadataId eq UUID.fromString(metadataId)
            }.limit(DB_BATCH_SIZE, offset).toList()
            yieldAll(result)
            offset += result.size
        } while (result.size == DB_BATCH_SIZE)
    }
}
