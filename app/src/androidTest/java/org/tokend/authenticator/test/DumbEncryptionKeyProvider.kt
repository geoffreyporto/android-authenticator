package org.tokend.authenticator.test

import io.reactivex.Single
import org.tokend.authenticator.security.encryption.logic.EncryptionKeyProvider
import org.tokend.kdf.ScryptKeyDerivation
import org.tokend.sdk.keyserver.models.KdfAttributes

class DumbEncryptionKeyProvider : EncryptionKeyProvider {

    override fun getKey(kdfAttributes: KdfAttributes): Single<ByteArray> {
        return Single.defer {
            Single.just(ScryptKeyDerivation(kdfAttributes.n, kdfAttributes.r, kdfAttributes.p)
                    .derive(tmpPass, kdfAttributes.salt!!, 32)
            )
        }
    }

    companion object {
        private val tmpPass = "1234".toByteArray()
    }
}