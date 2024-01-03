/*
 * Copyright (c) 2023-2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.cryptography.providers.tests.compatibility.api

import dev.whyoleg.cryptography.providers.tests.support.*
import dev.whyoleg.cryptography.testtool.api.*
import dev.whyoleg.cryptography.testtool.client.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.reflect.*

class ServerApi(
    private val algorithm: String,
    private val context: TestContext,
    private val logger: TestLogger,
) : CompatibilityApi() {
    private fun api(storageName: String): CompatibilityStorageApi = ServerStorageApi(algorithm, context, storageName, logger)
    override val keys: CompatibilityStorageApi = api("key")
    override val keyPairs: CompatibilityStorageApi = api("key-pair")
    override val digests: CompatibilityStorageApi = api("digest")
    override val signatures: CompatibilityStorageApi = api("signature")
    override val ciphers: CompatibilityStorageApi = api("cipher")
}


private class ServerStorageApi(
    private val algorithm: String,
    private val context: TestContext,
    storageName: String,
    logger: TestLogger,
) : CompatibilityStorageApi(storageName, logger) {
    private class ActorsContext(
        val currentActorId: TesttoolActorId,
        val contexts: Map<TesttoolActorId, TestContext>,
    )

    @OptIn(DelicateCoroutinesApi::class)
    private val actorsContext = GlobalScope.async {
        ActorsContext(
            TesttoolClient.saveActor(
                platform = TesttoolSerializer.encodeToJson(TestPlatform.serializer(), context.platform),
                provider = TesttoolSerializer.encodeToJson(TestProvider.serializer(), context.provider)
            ),
            TesttoolClient.getAllActors().toList().associate {
                it.id to TestContext(
                    TesttoolSerializer.decodeFromJson(TestPlatform.serializer(), it.platform),
                    TesttoolSerializer.decodeFromJson(TestProvider.serializer(), it.provider)
                )
            }
        )
    }

    override suspend fun <T : TestParameters> saveParameters(parameters: T, type: KType): String {
        val actorId = actorsContext.await().currentActorId
        val content = TesttoolSerializer.encodeToJson(type, parameters)
        return TesttoolClient.saveMetadata(actorId, algorithm, storageName, content)
    }

    override suspend fun <T : TestParameters> getParameters(type: KType): List<TestContent<T>> {
        return TesttoolClient.getAllMetadatas(algorithm, storageName).map {
            val content = TesttoolSerializer.decodeFromJson<T>(type, it.content)
            val context = actorsContext.await().contexts.getValue(it.createdBy)
            TestContent(it.id, content, context)
        }.toList()
    }

    override suspend fun <T : TestData> saveData(parametersId: TestParametersId, data: T, type: KType): String {
        val actorId = actorsContext.await().currentActorId
        val content = TesttoolSerializer.encodeToJson(type, data)
        return TesttoolClient.saveData(actorId, parametersId.value, content)
    }

    override suspend fun <T : TestData> getData(parametersId: TestParametersId, type: KType): List<TestContent<T>> {
        return TesttoolClient.getAllDatas(parametersId.value).map {
            val content = TesttoolSerializer.decodeFromJson<T>(type, it.content)
            val context = actorsContext.await().contexts.getValue(it.createdBy)
            TestContent(it.id, content, context)
        }.toList()
    }
}
