/*
 * Copyright (c) 2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.cryptography.testtool.server

import dev.whyoleg.cryptography.testtool.api.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

internal const val HTTP_BATCH_SIZE = 64

internal class TesttoolRequestHandler(
    private val server: TesttoolServer,
    private val scope: CoroutineScope,
) {

    private val requestIdCounter = AtomicInteger()
    private val streamRequests = ConcurrentHashMap<Int, ReceiveChannel<ByteArray>>()

    suspend fun handle(call: ApplicationCall) {
        val request = TesttoolSerializer.decodeFromBinary(
            TesttoolRequest.serializer(),
            call.receiveChannel().readRemaining().readBytes()
        )
        when (request) {
            is TesttoolRequest.ReturnsId     -> when (request) {
                is TesttoolRequest.SaveActor    -> call.respondText(
                    server.saveActor(request.platform, request.provider)
                )
                is TesttoolRequest.SaveMetadata -> call.respondText(
                    server.saveMetadata(request.actorId, request.algorithm, request.type, request.content)
                )
                is TesttoolRequest.SaveData     -> call.respondText(
                    server.saveData(request.actorId, request.metadataId, request.content)
                )
            }
            is TesttoolRequest.ReturnsStream -> when (request) {
                TesttoolRequest.GetAllActors       -> call.respondStream(server.getAllActors())
                is TesttoolRequest.GetAllMetadatas -> call.respondStream(server.getAllMetadatas(request.algorithm, request.type))
                is TesttoolRequest.GetAllDatas     -> call.respondStream(server.getAllDatas(request.metadataId))
            }
            is TesttoolRequest.GetNext       -> {
                call.respondBytes(streamRequests.getValue(request.requestId).receive())
            }
            is TesttoolRequest.Close         -> {
                streamRequests.remove(request.requestId)
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    private suspend inline fun <reified T> ApplicationCall.respondStream(flow: Flow<T>) {
        val requestId = requestIdCounter.incrementAndGet()
        val channel = flow.chunked(HTTP_BATCH_SIZE).map {
            TesttoolSerializer.encodeToBinary(
                TesttoolListChunk.serializer(serializer<T>()),
                TesttoolListChunk(requestId, it.size == HTTP_BATCH_SIZE, it)
            )
        }.produceIn(scope)
        streamRequests[requestId] = channel

        try {
            respondBytes(channel.receive())
        } catch (cause: Throwable) {
            streamRequests.remove(requestId)
            throw cause
        }
    }

    private fun <T> Flow<T>.chunked(size: Int): Flow<List<T>> {
        require(size > 0)

        return flow {
            val list = mutableListOf<T>()
            collect {
                list.add(it)

                if (list.size != size) return@collect

                emit(list.toList())
                list.clear()
            }
            // we push it even it's empty
            emit(list)
        }
    }
}
