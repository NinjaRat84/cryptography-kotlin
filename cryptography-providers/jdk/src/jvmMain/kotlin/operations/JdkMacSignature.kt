/*
 * Copyright (c) 2023-2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.cryptography.providers.jdk.operations

import dev.whyoleg.cryptography.operations.*
import dev.whyoleg.cryptography.providers.base.*
import dev.whyoleg.cryptography.providers.jdk.*

internal class JdkMacSignature(
    state: JdkCryptographyState,
    private val key: JSecretKey,
    algorithm: String,
) : SignatureGenerator, SignatureUpdaterGenerator,  SignatureVerifier {
    private val mac = state.mac(algorithm)

    private fun createFunction() = JdkMacFunction(mac.borrowResource { init(key) })

    override fun createSignFunction(): SignFunction = createFunction()
    override fun createVerifyFunction(): VerifyFunction = createFunction()
    override fun createSignUpdaterFunction(): SignUpdaterFunction = createFunction()
}

private class JdkMacFunction(
    private val mac: Pooled.Resource<JMac>,
) : SignFunction, SignUpdaterFunction, VerifyFunction {
    override fun update(source: ByteArray, startIndex: Int, endIndex: Int) {
        checkBounds(source.size, startIndex, endIndex)
        val mac = mac.access()
        mac.update(source, startIndex, endIndex - startIndex)
    }

    override fun process(source: ByteArray, iv: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun signIntoByteArray(destination: ByteArray, destinationOffset: Int): Int {
        val mac = mac.access()

        checkBounds(destination.size, destinationOffset, destinationOffset + mac.macLength)

        mac.doFinal(destination, destinationOffset)
        return mac.macLength
    }

    override fun signToByteArray(): ByteArray {
        val mac = mac.access()
        return mac.doFinal()
    }

    override fun tryVerify(signature: ByteArray, startIndex: Int, endIndex: Int): Boolean {
        checkBounds(signature.size, startIndex, endIndex)
        return signToByteArray().contentEquals(signature.copyOfRange(startIndex, endIndex))
    }

    override fun verify(signature: ByteArray, startIndex: Int, endIndex: Int) {
        check(tryVerify(signature, startIndex, endIndex)) { "Invalid signature" }
    }

    override fun reset() {
        mac.access().reset()
    }

    override fun close() {
        mac.close()
    }
}
