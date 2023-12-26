/*
 * Copyright (c) 2023 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.cryptography.providers.tests.behavior

import dev.whyoleg.cryptography.*
import dev.whyoleg.cryptography.BinarySize.Companion.bytes
import dev.whyoleg.cryptography.algorithms.asymmetric.*
import dev.whyoleg.cryptography.algorithms.digest.*
import dev.whyoleg.cryptography.materials.key.*
import dev.whyoleg.cryptography.providers.tests.support.*
import dev.whyoleg.cryptography.random.*
import kotlin.math.*
import kotlin.test.*

fun interface TestDataGenerator<D> {
    suspend fun SequenceScope<D>.generate()
}

fun interface TestDataValidator<D> {
    fun TestContext.validate(data: D): TestCheckResult
}

// TODO: scope
inline fun <D> generate(description: String, generator: TestDataGenerator<D>, block: (D) -> Unit) {}
inline fun <D> generate(description: String, generator: TestDataGenerator<D>, validator: TestDataValidator<D>, block: (D) -> Unit) {}
inline fun <D> matching(description: String, validator: TestDataValidator<D>, data: D, block: () -> Unit) {}
inline fun <D> group(description: String, block: () -> Unit) {}

data class RsaPssKeyGeneratorParameters(
    val keySize: BinarySize,
    val digest: CryptographyAlgorithmId<Digest>,
    val digestSize: Int,
)

private val RsaPssKeyParametersGenerator = TestDataGenerator {
    generateRsaKeySizes { keySize ->
        generateDigests { digest, digestSize ->
            yield(RsaPssKeyGeneratorParameters(keySize, digest, digestSize))
        }
    }
}

object RsaPssKeyParametersValidator : TestDataValidator<RsaPssKeyGeneratorParameters> {
    override fun TestContext.validate(data: RsaPssKeyGeneratorParameters): TestCheckResult {
        val s = data::digest
        val s2 = RsaPssKeyGeneratorParameters::digest

//        s.name
//        s2.name
//
//        composite {
//            add(data::digest, DigestValidator)
//        }
//
//        s.name
//        composite(
//            "digest", DigestValidator.validate(data.digest, context)
//        )
//        val digest = DigestValidator.validate(data.digest, context)
//        if (digest != ok())
    }
}

object DigestValidator : TestDataValidator<CryptographyAlgorithmId<Digest>> {
    private val sha3Algorithms = setOf(SHA3_224, SHA3_256, SHA3_384, SHA3_512)

    override fun TestContext.validate(data: CryptographyAlgorithmId<Digest>): TestCheckResult {
        when {
            provider.isWebCrypto  -> {
                if (data in sha3Algorithms || data == SHA224) return TestCheckResult.Unsupported()
            }
            provider.isApple      -> {
                if (data in sha3Algorithms) return TestCheckResult.Unsupported()
            }
            provider.isJdkDefault -> {
                if (data in sha3Algorithms) when {
                    platform.isJdk { major < 17 } -> return TestCheckResult.Unsupported("require min JDK 17")
                    platform.isAndroid            -> return TestCheckResult.Unsupported()
                }
            }
        }

        return TestCheckResult.Success
    }
}

object KeyFormatValidator : TestDataValidator<KeyFormat> {
    override fun TestContext.validate(data: KeyFormat): TestCheckResult {
        when {
            // only WebCrypto supports JWK for now
            data.name == "JWK" && !provider.isWebCrypto   -> return TestCheckResult.Unsupported()
            // drop this after migrating to kotlin Base64
            data.name == "PEM" &&
                    provider.isJdk &&
                    platform.isAndroid { apiLevel == 21 } -> return TestCheckResult.Unsupported("Android without Base64")
            // Apple provider doesn't have this formats out-of-the-box
            provider.isApple                              -> {
                if (data.name in setOf("PEM", "DER", "JWK")) return TestCheckResult.Unsupported()
            }
            // will be supported if ASN.1 serialization is ready
            // TODO JDK support, may be it's available
            provider.isJdk || provider.isWebCrypto        -> {
                if (data.name in setOf("PEM_RSA", "DER_RSA")) return TestCheckResult.Unsupported()
            }
        }

        return TestCheckResult.Success
    }
}

// skip: when for some reason, this check is skipped
// unsupported: when feature is not supported by provider
sealed class TestCheckResult {
    data object Success : TestCheckResult()
    data class Failure(val message: String) : TestCheckResult()
    data class Skip(val reason: String) : TestCheckResult()

    // when reason is null, then it's just unsupported
    data class Unsupported(val reason: String? = null) : TestCheckResult()
}

class RsaPssTest {

    @Test
    fun testSizes() = runTestForEachAlgorithm(RSA.PSS) {
        generate(
            "key params",
            RsaPssKeyParametersGenerator,
            RsaPssKeyParametersValidator
        ) { (keySize, digest, digestSize) ->
            val keyPair = algorithm.keyPairGenerator(keySize, digest).generateKey()

            matching("der encoding", KeyFormatValidator, RSA.PublicKey.Format.DER) {
                val encodedKey = keyPair.publicKey.encodeTo(RSA.PublicKey.Format.DER)
                assertEquals(keySize.inBytes + 38, encodedKey.size) {
                    "TAG or additional data"
                }
            }

            generate2(RsaPssSaltSizeGenerator(keySize, digestSize)) { saltSize ->
                val (signatureGenerator, signatureVerifier) = when (saltSize) {
                    null -> keyPair.privateKey.signatureGenerator() to keyPair.publicKey.signatureVerifier()
                    else -> keyPair.privateKey.signatureGenerator(saltSize.bytes) to keyPair.publicKey.signatureVerifier(saltSize.bytes)
                }

                checking(
                    "signature for empty array",
                    { signatureGenerator.generateSignature(ByteArray(0)) },
                    { checkEquals(keySize.inBytes, it.size) }
                )

                repeat(8) { n ->
                    val size = 10.0.pow(n).toInt()
                    val data = CryptographyRandom.nextBytes(size)
                    val signature = signatureGenerator.generateSignature(data)
                    assertEquals(keySize.inBytes, signature.size)
                    assertTrue(signatureVerifier.verifySignature(data, signature))
                }
            }
        }



        generateRsaKeySizes { keySize ->
            generateDigests { digest, digestSize ->
                if (!supportsDigest(digest)) return@generateDigests
                val keyPair = algorithm.keyPairGenerator(keySize, digest).generateKey()

                if (supportsKeyFormat(RSA.PublicKey.Format.DER)) {
                    assertEquals(keySize.inBytes + 38, keyPair.publicKey.encodeTo(RSA.PublicKey.Format.DER).size)
                }

                val maxSaltSize = (ceil((keySize.inBits - 1) / 8.0) - digestSize - 2).toInt()
                listOf(
                    null,
                    0,
                    CryptographyRandom.nextInt(1, digestSize),
                    digestSize,
                    CryptographyRandom.nextInt(digestSize, maxSaltSize),
                    maxSaltSize
                ).forEach { saltSize ->
                    if (!supportsSaltSize(saltSize)) return@forEach

                    val (signatureGenerator, signatureVerifier) = when (saltSize) {
                        null -> keyPair.privateKey.signatureGenerator() to keyPair.publicKey.signatureVerifier()
                        else -> keyPair.privateKey.signatureGenerator(saltSize.bytes) to keyPair.publicKey.signatureVerifier(saltSize.bytes)
                    }

                    assertEquals(keySize.inBytes, signatureGenerator.generateSignature(ByteArray(0)).size)
                    repeat(8) { n ->
                        val size = 10.0.pow(n).toInt()
                        val data = CryptographyRandom.nextBytes(size)
                        val signature = signatureGenerator.generateSignature(data)
                        assertEquals(keySize.inBytes, signature.size)
                        assertTrue(signatureVerifier.verifySignature(data, signature))
                    }
                }
            }
        }
    }
}
