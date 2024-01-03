/*
 * Copyright (c) 2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.cryptography.testtool.api

import kotlinx.serialization.*

@Serializable
public sealed interface TesttoolRequest {
    // for streaming
    @Serializable
    public data class GetNext(val requestId: Int) : TesttoolRequest

    @Serializable
    public data class Close(val requestId: Int) : TesttoolRequest

    public sealed interface ReturnsId : TesttoolRequest
    public sealed interface ReturnsStream : TesttoolRequest

    @Serializable
    public object GetAllActors : ReturnsStream

    @Serializable
    public data class SaveActor(
        val platform: JsonString,
        val provider: JsonString,
    ) : ReturnsId

    @Serializable
    public data class GetAllMetadatas(val algorithm: Algorithm, val type: DataType) : ReturnsStream

    @Serializable
    public data class SaveMetadata(
        val actorId: TesttoolActorId,
        val algorithm: Algorithm,
        val type: DataType,
        val content: JsonString,
    ) : ReturnsId

    @Serializable
    public data class GetAllDatas(val metadataId: TesttoolMetadataId) : ReturnsStream

    @Serializable
    public data class SaveData(
        val actorId: TesttoolActorId,
        val metadataId: TesttoolMetadataId,
        val content: JsonString,
    ) : ReturnsId
}
