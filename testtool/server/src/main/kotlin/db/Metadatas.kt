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

internal object Metadatas : UUIDTable("metadatas") {
    val instanceId = text("instance_id")
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val createdBy = reference("created_by", Actors)

    val algorithm = text("algorithm")
    val dataType = text("data_type")
    val content = text("content")

    fun save(
        instanceId: String,
        createdBy: String,
        algorithm: String,
        dataType: String,
        content: String,
    ): EntityID<UUID> = insertAndGetId {
        it[this.instanceId] = instanceId
        it[this.createdBy] = UUID.fromString(createdBy)
        it[this.algorithm] = algorithm
        it[this.dataType] = dataType
        it[this.content] = content
    }

    fun dataType(id: String): String = select { Metadatas.id eq UUID.fromString(id) }.first()[dataType]

    fun getAll(
        algorithm: String,
        dataType: String,
    ): Sequence<ResultRow> = sequence {
        var offset = 0L
        do {
            val result = select {
                (Metadatas.algorithm eq algorithm) and (Metadatas.dataType eq dataType)
            }.limit(DB_BATCH_SIZE, offset).toList()
            yieldAll(result)
            offset += result.size
        } while (result.size == DB_BATCH_SIZE)
    }
}
