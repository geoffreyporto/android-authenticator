package org.tokend.authenticator.accounts.data.storage

import org.tokend.authenticator.accounts.data.model.Account
import org.tokend.authenticator.accounts.data.model.db.AccountEntity
import org.tokend.authenticator.logic.db.AppDatabase
import org.tokend.authenticator.base.repository.RepositoryCache

class AccountsCache(database: AppDatabase) : RepositoryCache<Account>() {
    private val dao = database.accountsDao

    override fun isContentSame(first: Account, second: Account): Boolean {
        return first.network == second.network
                && first.email == second.email
                && first.originalAccountId == second.originalAccountId
                && first.walletId == second.walletId
                && first.encryptedSeed.cipherText.contentEquals(second.encryptedSeed.cipherText)
                && first.kdfAttributes.encodedSalt == second.kdfAttributes.encodedSalt
    }

    override fun getAllFromDb(): List<Account> {
        return dao.getAll()
                .map {
                    it.toAccount()
                }
    }

    override fun addToDb(items: List<Account>) {
        dao.insert(*items.map { AccountEntity.fromAccount(it) }.toTypedArray())
    }

    override fun updateInDb(items: List<Account>) {
        dao.update(*items.map { AccountEntity.fromAccount(it) }.toTypedArray())
    }

    override fun deleteFromDb(items: List<Account>) {
        dao.delete(*items.map { AccountEntity.fromAccount(it) }.toTypedArray())
    }

    override fun clearDb() {
        dao.deleteAll()
    }
}