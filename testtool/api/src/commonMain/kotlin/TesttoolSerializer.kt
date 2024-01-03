/*
 * Copyright (c) 2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.cryptography.testtool.api

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.protobuf.*
import kotlin.io.encoding.*
import kotlin.reflect.*

@OptIn(ExperimentalSerializationApi::class)
public object TesttoolSerializer {
    private val binary = ProtoBuf
    private val json = Json {
        serializersModule = SerializersModule {
            contextual(Base64ByteArraySerializer)
        }
    }

    public fun <T> encodeToJson(type: KType, value: T): String = json.encodeToString(
        json.serializersModule.serializer(type),
        value
    )

    @Suppress("UNCHECKED_CAST")
    public fun <T> decodeFromJson(type: KType, string: String): T = json.decodeFromString(
        json.serializersModule.serializer(type) as KSerializer<T>,
        string
    )

    public fun <T> encodeToJson(serializer: KSerializer<T>, value: T): String = json.encodeToString(serializer, value)
    public fun <T> decodeFromJson(serializer: KSerializer<T>, string: String): T = json.decodeFromString(serializer, string)

    public fun <T> encodeToBinary(serializer: KSerializer<T>, value: T): ByteArray = binary.encodeToByteArray(serializer, value)
    public fun <T> decodeFromBinary(serializer: KSerializer<T>, byteArray: ByteArray): T = binary.decodeFromByteArray(serializer, byteArray)
}

public typealias Base64ByteArray = @Contextual ByteArray

@OptIn(ExperimentalEncodingApi::class)
private object Base64ByteArraySerializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Base64", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ByteArray {
        return Base64.decode(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder.encodeString(Base64.encode(value))
    }
}
