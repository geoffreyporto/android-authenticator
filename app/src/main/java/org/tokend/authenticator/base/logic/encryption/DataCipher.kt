package org.tokend.authenticator.base.logic.encryption

import io.reactivex.Single
import org.tokend.sdk.keyserver.models.KeychainData

interface DataCipher {
    fun encrypt(data: ByteArray, key: ByteArray): Single<KeychainData>

    fun decrypt(encryptedData: KeychainData, key: ByteArray): Single<ByteArray>
}