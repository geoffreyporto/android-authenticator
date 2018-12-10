package org.tokend.authenticator.security.view.activities

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.view.View
import kotlinx.android.synthetic.main.activity_pin_code.*
import org.tokend.authenticator.R
import org.tokend.authenticator.base.extensions.getChars
import org.tokend.authenticator.base.extensions.setErrorAndFocus
import org.tokend.authenticator.base.util.SoftInputUtil
import org.tokend.authenticator.base.util.ToastManager
import org.tokend.authenticator.base.view.util.AnimationUtil
import org.tokend.authenticator.base.view.util.SimpleTextWatcher
import org.tokend.authenticator.security.view.UserKeyActivity
import org.tokend.wallet.utils.toCharArray
import java.nio.CharBuffer
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

open class PinCodeActivity : UserKeyActivity() {
    protected val PIN_CODE_STORAGE_KEY = "pin"

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_pin_code)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initFields()
        initTimerLayout()
    }

    private fun initFields() {
        pin_code_edit_text.filters = arrayOf(
                InputFilter.LengthFilter(PIN_CODE_LENGTH)
        )

        pin_code_edit_text.addTextChangedListener(
                object : SimpleTextWatcher() {
                    override fun afterTextChanged(s: Editable?) {
                        if (s != null && s.length == PIN_CODE_LENGTH) {
                            onPinCodeEntered(s.getChars())
                        }
                    }
                }
        )
    }

    protected open fun onPinCodeEntered(pin: CharArray) {
        SoftInputUtil.hideSoftInput(this)
        finishWithKey(pin)
    }

    private fun initTimerLayout() {
        if (!punishmentTimer.isExpired()) {
            supportActionBar?.hide()
            SoftInputUtil.hideSoftInput(this)
            timer_holder.visibility = View.VISIBLE
            val timerTemplate = getString(R.string.template_timer)

            var timeLeft = punishmentTimer.timeLeft()
            timer_text.text = timerTemplate.format(timeLeft.toString())

            Timer().scheduleAtFixedRate(1000, 1000) {
                runOnUiThread {
                    timeLeft--
                    timer_text.text = timerTemplate.format(timeLeft.toString())
                    if(timeLeft == 0) {
                        timer_holder.visibility = View.GONE
                        supportActionBar?.show()
                        focusOnEditText(true)
                        requestFingerprintAuthIfAvailable()
                        cancel()
                    }
                }
            }

        } else {
            focusOnEditText()
            requestFingerprintAuthIfAvailable()
        }
    }

    private fun focusOnEditText(force: Boolean = false) {
        pin_code_edit_text.isFocusableInTouchMode = true
        val retry = intent.getBooleanExtra(IS_RETRY_EXTRA, false) || force
        if(retry) {
            pin_code_edit_text.setErrorAndFocus(getString(R.string.invalid_pin))
        } else {
            SoftInputUtil.showSoftInputOnView(pin_code_edit_text)
        }
    }

    // region Fingerprint
    protected open fun requestFingerprintAuthIfAvailable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && secureStorage.hasSecretKey(PIN_CODE_STORAGE_KEY)
                && fingerprintUtil.canFingerPrintBeUsed()) {
            requestFingerprintAuth()
        }
    }

    protected open fun requestFingerprintAuth() {
        fingerprintUtil.requestAuth(
                onSuccess = this::onFingerprintAuthSuccess,
                onError = this::onFingerprintAuthMessage,
                onHelp = this::onFingerprintAuthMessage
        )
        showFingerprintHint()
    }

    protected open fun cancelFingerprintAuth() {
        fingerprintUtil.cancelAuth()
    }

    protected open fun onFingerprintAuthSuccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        val savedPinCode = secureStorage.load(PIN_CODE_STORAGE_KEY)?.toCharArray()
                ?: return

        pin_code_edit_text.text.apply {
            replace(0, length, CharBuffer.wrap(savedPinCode))
        }
    }

    protected open fun onFingerprintAuthMessage(message: String?) {
        message ?: return
        ToastManager(this).short(message)
    }

    protected open fun showFingerprintHint() {
        AnimationUtil.fadeInView(fingerprint_hint_layout)
    }

    protected open fun hideFingerprintHint() {
        fingerprint_hint_layout.visibility = View.GONE
    }
    // endregion

    override fun onPause() {
        super.onPause()
        cancelFingerprintAuth()
    }

    companion object {
        protected const val PIN_CODE_LENGTH = 4
    }
}
