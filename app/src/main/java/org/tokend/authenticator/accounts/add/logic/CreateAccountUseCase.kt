package org.tokend.authenticator.accounts.add.logic

import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import okhttp3.HttpUrl
import org.tokend.authenticator.accounts.data.model.Account
import org.tokend.authenticator.accounts.data.model.Network
import org.tokend.authenticator.accounts.data.storage.AccountsRepository
import org.tokend.authenticator.logic.api.factory.ApiFactory
import org.tokend.authenticator.logic.wallet.WalletManager
import org.tokend.authenticator.security.encryption.cipher.DataCipher
import org.tokend.authenticator.security.encryption.logic.EncryptionKeyProvider
import org.tokend.authenticator.security.encryption.logic.KdfAttributesGenerator
import org.tokend.authenticator.util.extensions.toSingle
import org.tokend.crypto.ecdsa.erase
import org.tokend.rx.extensions.toCompletable
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.general.model.SystemInfo
import org.tokend.sdk.keyserver.models.KdfAttributes
import org.tokend.sdk.keyserver.models.WalletData
import org.tokend.wallet.utils.toByteArray

/**
 * Create account, submit it to the system,
 * encrypt it and save in the repo.
 */
class CreateAccountUseCase(
        networkUrl: String,
        private val email: String,
        private val cipher: DataCipher,
        private val encryptionKeyProvider: EncryptionKeyProvider,
        private val accountsRepository: AccountsRepository,
        apiFactory: ApiFactory
) {
    class Result(
            val account: Account,
            val recoverySeed: CharArray
    )

    private val networkUrl = HttpUrl.parse(networkUrl).toString()

    private val api = apiFactory.getApi(networkUrl)
    private val keyStorageApi = apiFactory.getKeyServer(networkUrl)

    private lateinit var kdfAttributes: KdfAttributes
    private lateinit var network: Network
    private lateinit var masterKeyPair: org.tokend.wallet.Account
    private lateinit var recoveryKeyPair: org.tokend.wallet.Account
    private lateinit var walletId: String

    fun perform(): Single<Result> {
        val scheduler = Schedulers.newThread()

        return getKdfAttributes()
                .observeOn(scheduler)
                .doOnSuccess { kdfAttributes ->
                    this.kdfAttributes = kdfAttributes
                }
                // Get network information.
                .flatMap {
                    getSystemInfo()
                }
                .observeOn(scheduler)
                .map { systemInfo ->
                    Network.fromSystemInfo(networkUrl, systemInfo)
                }
                .observeOn(scheduler)
                .doOnSuccess { network ->
                    this.network = network
                }
                // Generate master and recovery keys.
                .flatMap {
                    Single.zip(
                            getRandomKeyPair(),
                            getRandomKeyPair(),
                            BiFunction { x: org.tokend.wallet.Account,
                                         y: org.tokend.wallet.Account ->
                                x to y
                            }
                    )
                }
                .observeOn(scheduler)
                .doOnSuccess { (masterKeyPair, recoveryKeyPair) ->
                    this.masterKeyPair = masterKeyPair
                    this.recoveryKeyPair = recoveryKeyPair
                }
                // Generate TokenD wallet.
                .flatMap {
                    getWallet()
                }
                .observeOn(scheduler)
                .doOnSuccess { wallet ->
                    this.walletId = wallet.id!!
                }
                // Post wallet to the system.
                .flatMap { wallet ->
                    postWallet(wallet)
                }
                .observeOn(scheduler)
                // Create account.
                .flatMap {
                    createAccount()
                }
                .observeOn(scheduler)
                // Save it finally.
                .doOnSuccess { account ->
                    accountsRepository.add(account)
                }
                .map { account ->
                    Result(
                            account = account,
                            recoverySeed = recoveryKeyPair.secretSeed!!
                    )
                }
                .doOnEvent { _, _ ->
                    destroyKeys()
                }
    }

    private fun getKdfAttributes(): Single<KdfAttributes> {
        return keyStorageApi
                .getLoginParams()
                .toSingle()
                .map {
                    it.kdfAttributes.also { kdf ->
                        kdf.salt = KdfAttributesGenerator().getRandomSalt()
                    }
                }
    }

    private fun getSystemInfo(): Single<SystemInfo> {
        return api.general.getSystemInfo().toSingle()
    }

    private fun getRandomKeyPair(): Single<org.tokend.wallet.Account> {
        return {
            org.tokend.wallet.Account.random()
        }.toSingle()
    }

    private fun getWallet(): Single<WalletData> {
        return WalletManager.createWallet(
                email,
                masterKeyPair,
                recoveryKeyPair,
                kdfAttributes
        )
    }

    private fun postWallet(wallet: WalletData): Single<Boolean> {
        return keyStorageApi
                .saveWallet(wallet)
                .toCompletable()
                .toSingleDefault(true)
    }

    private fun createAccount(): Single<Account> {
        return encryptionKeyProvider.getKey(kdfAttributes)
                .flatMap { encryptionKey ->
                    cipher.encrypt(masterKeyPair.secretSeed!!.toByteArray(), encryptionKey)
                            .doOnSuccess {
                                encryptionKey.erase()
                            }
                }
                .map { encryptedSeed ->
                    Account(
                            network = network,
                            email = email,
                            originalAccountId = masterKeyPair.accountId,
                            walletId = walletId,
                            publicKey = masterKeyPair.accountId,
                            encryptedSeed = encryptedSeed,
                            kdfAttributes = kdfAttributes
                    )
                }
    }

    private fun destroyKeys() {
        try {
            masterKeyPair.destroy()
            recoveryKeyPair.destroy()
        } catch (e: UninitializedPropertyAccessException) {
            // Doesn't matter
        }
    }
}