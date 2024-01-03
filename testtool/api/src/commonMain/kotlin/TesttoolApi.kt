/*
 * Copyright (c) 2023-2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.cryptography.testtool.api

import kotlinx.coroutines.flow.*

public interface TesttoolApi {
    public fun getAllActors(): Flow<TesttoolActor>
    public suspend fun saveActor(
        platform: JsonString,
        provider: JsonString,
    ): TesttoolActorId

    public fun getAllMetadatas(algorithm: Algorithm, type: DataType): Flow<TesttoolMetadata>
    public suspend fun saveMetadata(
        actorId: TesttoolActorId,
        algorithm: Algorithm,
        type: DataType,
        content: JsonString,
    ): TesttoolMetadataId

    public fun getAllDatas(metadataId: TesttoolMetadataId): Flow<TesttoolData>
    public suspend fun saveData(
        actorId: TesttoolActorId,
        metadataId: TesttoolMetadataId,
        content: JsonString,
    ): TesttoolDataId
}
