/*
 * Copyright (c) 2023-2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.cryptography.testtool.api

import kotlinx.serialization.*

// util types
public typealias Algorithm = String
public typealias JsonString = String
public typealias DataType = String

public typealias TesttoolActorId = String

@Serializable
public data class TesttoolActor(
    val id: TesttoolActorId,
    val platform: JsonString,
    val provider: JsonString,
)

public typealias TesttoolMetadataId = String

@Serializable
public data class TesttoolMetadata(
    val id: TesttoolMetadataId,
    val createdBy: TesttoolActorId,
    val algorithm: Algorithm,
    val dataType: DataType,
    val content: JsonString,
)

public typealias TesttoolDataId = String

@Serializable
public data class TesttoolData(
    val id: TesttoolDataId,
    val createdBy: TesttoolActorId,
    val metadataId: TesttoolMetadataId,
    val content: JsonString,
)

@Serializable
public data class TesttoolListChunk<T>(
    val requestId: Int,
    val hasMore: Boolean,
    val elements: List<T>,
)
