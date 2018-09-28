package org.tokend.authenticator.base.util

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.view.View
import org.jetbrains.anko.clearTop
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.newTask
import org.jetbrains.anko.noHistory
import org.tokend.authenticator.R
import org.tokend.authenticator.base.activities.RecoveryActivity
import org.tokend.authenticator.base.activities.RecoverySeedActivity
import org.tokend.authenticator.base.activities.account_list.AccountsListActivity
import org.tokend.authenticator.base.activities.add_account.AddAccountActivity


/**
 * Performs transitions between screens.
 * 'open-' will open related screen as a child.<p>
 * 'to-' will open related screen and finish current.
 */
object Navigator {

    private fun fadeOut(activity: Activity) {
        ActivityCompat.finishAfterTransition(activity)
        activity.overridePendingTransition(0, R.anim.activity_fade_out)
        activity.finish()
    }

    private fun createTransitionBundle(activity: Activity,
                                       vararg pairs: Pair<View?, String>): Bundle {
        val sharedViews = arrayListOf<android.support.v4.util.Pair<View, String>>()

        pairs.forEach {
            val view = it.first
            if (view != null) {
                sharedViews.add(android.support.v4.util.Pair(view, it.second))
            }
        }

        return if (sharedViews.isEmpty()) {
            Bundle.EMPTY
        } else {
            ActivityOptionsCompat.makeSceneTransitionAnimation(activity,
                    *sharedViews.toTypedArray()).toBundle() ?: Bundle.EMPTY
        }
    }

    fun openAddAccount(activity: Activity) {
        activity.startActivity(activity.intentFor<AddAccountActivity>())
    }

    fun openRecoverySeedSaving(activity: Activity, requestCode: Int, seed: String) {
        activity.startActivityForResult(activity.intentFor<RecoverySeedActivity>(
                RecoverySeedActivity.SEED_EXTRA to seed
        ), requestCode)
    }

    fun openRecoveryActivity(activity: Activity, api: String, email: String) {
        activity.startActivity(activity.intentFor<RecoveryActivity>(
                RecoveryActivity.EXTRA_API to api,
                RecoveryActivity.EXTRA_EMAIL to email
        ))
    }

    fun toAccountsList(activity: Activity) {
        activity.startActivity(activity.intentFor<AccountsListActivity>().clearTop())
    }
}