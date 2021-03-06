package org.tokend.authenticator.settings.view

import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.preference.ListPreference
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.View
import org.tokend.authenticator.App
import org.tokend.authenticator.R
import org.tokend.authenticator.view.decoration.PreferenceDividerDecoration
import org.tokend.authenticator.security.environment.logic.EnvSecurityStatusProvider
import javax.inject.Inject

abstract class SettingsFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {
    @Inject
    lateinit var envSecurityStatusProvider: EnvSecurityStatusProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity?.application as? App)?.appComponent?.inject(this)

        reloadPreferences()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {}

    protected open fun reloadPreferences() {
        preferenceScreen = null

        setPreferencesFromResource(R.xml.preferences, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.white, null))

        listView.addItemDecoration(
                PreferenceDividerDecoration(context,
                        R.drawable.line_divider, R.dimen.divider_height)
                        .setPaddingLeft(resources
                                .getDimensionPixelSize(R.dimen.divider_with_icon_padding_left))
                        .drawBetweenItems(true)
                        .drawTop(false)
                        .drawBottom(false)
                        .drawBetweenCategories(false))
    }

    protected fun updateSummary(key: String, value: String) {
        val preference = findPreference(key)
        if (preference != null) {
            preference.summary = value
        }
    }

    protected fun updateSummary(key: String) {
        val preference = findPreference(key)
        if (preference != null && preference is ListPreference) {
            val entry = preference.entry
            if (entry != null) {
                updateSummary(key, entry.toString())
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        updateSummary(key)
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences
                .unregisterOnSharedPreferenceChangeListener(this)
    }
}