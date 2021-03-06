package org.tokend.authenticator.accounts.info.data.storage

import android.support.v4.util.LruCache
import org.tokend.authenticator.accounts.data.model.Account
import org.tokend.authenticator.logic.api.factory.ApiFactory
import org.tokend.authenticator.logic.db.AppDatabase

class AccountSignersRepositoryProvider(
        private val database: AppDatabase,
        private val apiFactory: ApiFactory
) {
    private val repoByAccountId = LruCache<Long, AccountSignersRepository>(MAX_REPOSITORIES_COUNT)

    fun getForAccount(account: Account): AccountSignersRepository {
        val accountId = account.uid
        return repoByAccountId[accountId]
                ?: AccountSignersRepository(
                        account,
                        apiFactory,
                        AccountSignersCache(accountId, database)
                )
                        .also { repoByAccountId.put(accountId, it) }
    }

    companion object {
        private const val MAX_REPOSITORIES_COUNT = 10
    }
}