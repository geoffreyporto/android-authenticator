package org.tokend.authenticator.base.logic.transactions

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.tokend.authenticator.base.extensions.toSingle
import org.tokend.sdk.api.ApiService
import org.tokend.sdk.api.responses.SubmitTransactionResponse
import org.tokend.sdk.factory.GsonFactory
import org.tokend.wallet.*
import org.tokend.wallet.xdr.Operation
import retrofit2.HttpException
import java.net.HttpURLConnection
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class TxManager(
        private val api: ApiService
) {
    fun submit(transaction: Transaction): Single<SubmitTransactionResponse> {
        return api.pushTransaction(transaction.getEnvelope().toBase64())
                .toSingle()
                // Wrap failed submit response into special exception.
                .onErrorResumeNext {
                    if (it is HttpException
                            && it.code() == HttpURLConnection.HTTP_BAD_REQUEST) {
                        val submitResponse = responseFromErrorBody(it.response().errorBody())
                        if (submitResponse != null) {
                            Single.error(TransactionFailedException(submitResponse))
                        } else {
                            Single.error<SubmitTransactionResponse>(it)
                        }
                    } else {
                        Single.error<SubmitTransactionResponse>(it)
                    }
                }
                // Magic delay is required because
                // API doesn't sync with Horizon immediately.
                .delay(1, TimeUnit.SECONDS)
    }

    private fun responseFromErrorBody(errorBody: ResponseBody): SubmitTransactionResponse? {
        val buffer = errorBody.source().buffer().clone()
        val string = buffer.readString(Charset.defaultCharset())
        return try {
            GsonFactory().getBaseGson().fromJson(string, SubmitTransactionResponse::class.java)
        } catch (e: Exception) {
            null
        } finally {
            buffer.close()
        }
    }

    companion object {
        fun createSignedTransaction(networkParams: NetworkParams,
                                    sourceAccountId: String,
                                    signer: Account,
                                    vararg operations: Operation.OperationBody
        ): Single<Transaction> {
            return Single.defer {
                val transaction =
                        TransactionBuilder(networkParams,
                                PublicKeyFactory.fromAccountId(sourceAccountId))
                                .apply {
                                    operations.forEach {
                                        addOperation(it)
                                    }
                                }
                                .build()

                transaction.addSignature(signer)

                Single.just(transaction)
            }.subscribeOn(Schedulers.newThread())
        }
    }
}