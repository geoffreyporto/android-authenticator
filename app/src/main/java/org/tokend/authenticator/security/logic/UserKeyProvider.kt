package org.tokend.authenticator.security.logic

import io.reactivex.Maybe

interface UserKeyProvider {
    fun getUserKey(): Maybe<CharArray>
}