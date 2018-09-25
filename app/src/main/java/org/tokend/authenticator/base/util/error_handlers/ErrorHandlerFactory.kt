package org.tokend.authenticator.base.util.error_handlers

import android.content.Context

class ErrorHandlerFactory(
        private val context: Context
) {
    private val defaultErrorHandler: ErrorHandler by lazy {
        DefaultErrorHandler(context)
    }

    fun getDefault(): ErrorHandler {
        return defaultErrorHandler
    }
}