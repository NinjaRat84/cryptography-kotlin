/*
 * Copyright (c) 2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.cryptography.testtool.server

import dev.whyoleg.cryptography.testtool.api.*
import dev.whyoleg.cryptography.testtool.server.db.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.*

internal class TesttoolServer(
    private val instanceId: String,
    private val db: Database,
) : TesttoolApi {
    override suspend fun saveActor(platform: JsonString, provider: JsonString): TesttoolActorId = tx {
        Actors.save(instanceId, platform, provider).value.toString()
    }

    // TODO: single tx may be bad (or may be make it RO)
    override fun getAllActors(): Flow<TesttoolActor> = txFlow {
        Actors.getAll().forEach {
            send(
                TesttoolActor(
                    id = it[Actors.id].value.toString(),
                    platform = it[Actors.platform],
                    provider = it[Actors.provider]
                )
            )
        }
    }

    override suspend fun saveMetadata(
        actorId: TesttoolActorId,
        algorithm: Algorithm,
        type: DataType,
        content: JsonString,
    ): TesttoolMetadataId = tx {
        Metadatas.save(instanceId, actorId, algorithm, type, content).value.toString()
    }

    override fun getAllMetadatas(algorithm: Algorithm, type: DataType): Flow<TesttoolMetadata> = txFlow {
        Metadatas.getAll(algorithm, type).forEach {
            send(
                TesttoolMetadata(
                    id = it[Metadatas.id].value.toString(),
                    createdBy = it[Metadatas.createdBy].value.toString(),
                    algorithm = it[Metadatas.algorithm],
                    dataType = it[Metadatas.dataType],
                    content = it[Metadatas.content],
                )
            )
        }
    }

    override suspend fun saveData(actorId: TesttoolActorId, metadataId: TesttoolMetadataId, content: JsonString): TesttoolDataId = tx {
        Datas.save(instanceId, actorId, metadataId, content).value.toString()
    }

    override fun getAllDatas(metadataId: TesttoolMetadataId): Flow<TesttoolData> = txFlow {
        Datas.getAll(metadataId).forEach {
            send(
                TesttoolData(
                    id = it[Datas.id].value.toString(),
                    createdBy = it[Datas.createdBy].value.toString(),
                    metadataId = it[Datas.metadataId].value.toString(),
                    content = it[Datas.content]
                )
            )
        }
    }

    private suspend inline fun <T> tx(crossinline statement: suspend () -> T): T = newSuspendedTransaction(db = db) { statement() }
    private inline fun <T> txFlow(crossinline statement: suspend ProducerScope<T>.() -> Unit): Flow<T> = channelFlow {
        newSuspendedTransaction(db = db) {
            statement(this@channelFlow)
        }
    }
}
