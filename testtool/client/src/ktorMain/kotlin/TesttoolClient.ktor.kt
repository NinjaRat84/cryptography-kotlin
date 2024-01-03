/*
 * Copyright (c) 2023-2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.cryptography.testtool.client

import dev.whyoleg.cryptography.testtool.api.*
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.content.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*

public actual val TesttoolClient: TesttoolApi = TesttoolApiImpl

private object TesttoolApiImpl : TesttoolApi {
    override suspend fun saveActor(platform: JsonString, provider: JsonString): TesttoolActorId {
        return requestId(TesttoolRequest.SaveActor(platform, provider))
    }

    override fun getAllActors(): Flow<TesttoolActor> {
        return requestFlow(TesttoolRequest.GetAllActors)
    }

    override suspend fun saveMetadata(
        actorId: TesttoolActorId,
        algorithm: Algorithm,
        type: DataType,
        content: JsonString,
    ): TesttoolMetadataId {
        return requestId(TesttoolRequest.SaveMetadata(actorId, algorithm, type, content))
    }

    override fun getAllMetadatas(algorithm: Algorithm, type: DataType): Flow<TesttoolMetadata> {
        return requestFlow(TesttoolRequest.GetAllMetadatas(algorithm, type))
    }

    override suspend fun saveData(actorId: TesttoolActorId, metadataId: TesttoolMetadataId, content: JsonString): TesttoolDataId {
        return requestId(TesttoolRequest.SaveData(actorId, metadataId, content))
    }

    override fun getAllDatas(metadataId: TesttoolMetadataId): Flow<TesttoolData> {
        return requestFlow(TesttoolRequest.GetAllDatas(metadataId))
    }

    // impl details

    private val client = HttpClient {
        expectSuccess = true
        install(DefaultRequest) {
            host = hostOverride() ?: ""
            port = 9000
        }
        install(HttpRequestRetry)
    }

    private suspend inline fun <T : TesttoolRequest> post(request: T): HttpResponse = client.post {
        setBody(ByteArrayContent(TesttoolSerializer.encodeToBinary(TesttoolRequest.serializer(), request)))
    }

    private suspend inline fun <T : TesttoolRequest> requestId(request: T): String = post(request).bodyAsText()

    private suspend inline fun <T : TesttoolRequest, reified R : Any> request(request: T): R =
        TesttoolSerializer.decodeFromBinary(serializer<R>(), post(request).readBytes())

    private inline fun <T : TesttoolRequest, reified R : Any> requestFlow(request: T): Flow<R> = flow {
        val firstResponse = request<_, TesttoolListChunk<R>>(request)
        val requestId = firstResponse.requestId
        try {
            firstResponse.elements.forEach { emit(it) }
            if (!firstResponse.hasMore) return@flow

            val requestNext = TesttoolRequest.GetNext(requestId)
            do {
                val response = request<_, TesttoolListChunk<R>>(requestNext)
                response.elements.forEach { emit(it) }
            } while (response.hasMore)
        } catch (cause: Throwable) {
            withContext(NonCancellable) {
                post(TesttoolRequest.Close(requestId))
            }
            throw cause
        }
    }
}
