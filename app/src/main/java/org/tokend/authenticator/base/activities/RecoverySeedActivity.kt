package org.tokend.authenticator.base.activities

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.InputType
import kotlinx.android.synthetic.main.activity_recovery_seed.*
import org.jetbrains.anko.clipboardManager
import org.jetbrains.anko.dip
import org.tokend.authenticator.R
import org.tokend.authenticator.base.extensions.getNullableStringExtra
import org.tokend.authenticator.base.extensions.onEditorAction
import org.tokend.authenticator.base.util.ToastManager
import org.tokend.authenticator.base.view.util.SimpleTextWatcher


class RecoverySeedActivity : AppCompatActivity() {
    companion object {
        const val SEED_EXTRA = "seed"
    }

    private val seed: String?
        get() = intent.getNullableStringExtra(SEED_EXTRA)

    private var canContinue: Boolean = false
        set(value) {
            field = value
            continue_button.isEnabled = value
        }

    private var seedsMatch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (seed == null) {
            finish()
            return
        }

        setContentView(R.layout.activity_recovery_seed)

        initFields()
        initButtons()

        canContinue = false
    }

    // region Init
    private fun initFields() {
        seed_edit_text.apply {
            setText(seed)
            setSingleLine()
            setPaddings(0, 0, dip(40), 0)
            inputType = InputType.TYPE_NULL
            setTextIsSelectable(true)
        }

        confirm_seed_edit_text.apply {
            addTextChangedListener(object : SimpleTextWatcher() {
                override fun afterTextChanged(s: Editable?) {
                    checkSeedsMatch()
                    updateContinueAvailability()
                }
            })
            requestFocus()
            onEditorAction {
                if (canContinue) {
                    finishWithSuccess()
                }
            }
        }
    }

    private fun initButtons() {
        continue_button.setOnClickListener {
            finishWithSuccess()
        }

        copy_button.setOnClickListener {
            clipboardManager.text = seed
            ToastManager(this).short(R.string.seed_copied)
        }
    }
    // endregion

    private fun checkSeedsMatch() {
        seedsMatch = seed_edit_text.text.toString() ==
                confirm_seed_edit_text.text.toString()

        if (!seedsMatch && !confirm_seed_edit_text.text.isEmpty()) {
            confirm_seed_edit_text.error = getString(R.string.error_seed_mismatch)
        } else {
            confirm_seed_edit_text.error = null
        }
    }

    private fun updateContinueAvailability() {
        canContinue = seedsMatch
    }

    private fun finishWithSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun displaySkipConfirmation() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.are_you_sure)
                .setMessage(R.string.seed_not_copied_confirmation)
                .setPositiveButton(R.string.i_understand, null)
                .show()
    }

    override fun onBackPressed() {
        if (seedsMatch || clipboardManager.text?.equals(seed) == true) {
            finishWithSuccess()
        } else {
            displaySkipConfirmation()
        }
    }
}