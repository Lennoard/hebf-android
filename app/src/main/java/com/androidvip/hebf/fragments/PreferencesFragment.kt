package com.androidvip.hebf.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.androidvip.hebf.R
import com.androidvip.hebf.lower
import com.androidvip.hebf.runSafeOnUiThread
import com.androidvip.hebf.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class PreferencesFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val workerContext: CoroutineContext = Dispatchers.Default + SupervisorJob()

    private val userPrefs: SharedPreferences by lazy { UserPrefs(requireContext()).preferences }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        userPrefs.registerOnSharedPreferenceChangeListener(this)

        if (userPrefs.getInt(K.PREF.USER_TYPE, K.USER_TYPE_NORMAL) < K.USER_TYPE_EXPERT) {
            findPreference<Preference>(K.PREF.EXTENDED_LOGGING_ENABLED)?.isVisible = false
        }

        val busyboxPathPreference = findPreference<Preference>("busybox_path")
        val suPathPreference = findPreference<Preference>("su_path")

        lifecycleScope.launch(workerContext) {
            val busyboxPath = RootUtils.executeWithOutput(
                "which busybox", "", activity
            ).lower()
            val suPath = RootUtils.executeWithOutput(
                "which su",
                "",
                activity
            ).lower()

            activity?.runSafeOnUiThread {
                busyboxPathPreference?.summary = if (busyboxPath.contains("not found") || busyboxPath.isEmpty()) {
                    getString(android.R.string.unknownName)
                } else {
                    busyboxPath
                }

                suPathPreference?.summary = if (suPath.lower().contains("not found")) {
                    getString(android.R.string.unknownName)
                } else {
                    suPath
                }
            }
        }

        findPreference<Preference>("notification_settings")?.apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                isVisible = false
                isEnabled = false
            } else {
                setOnPreferenceClickListener {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                        startActivity(this)
                    }
                    true
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        try {
            if (key == K.PREF.THEME) {
                preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
                val e = userPrefs.edit()
                when (sharedPreferences.getString(key, Themes.LIGHT)) {
                    Themes.LIGHT -> {
                        e.putString(K.PREF.THEME, Themes.LIGHT).apply()
                    }
                    Themes.DARKNESS -> {
                        e.putString(K.PREF.THEME, Themes.DARKNESS).apply()
                    }
                    Themes.AMOLED -> {
                        e.putString(K.PREF.THEME, Themes.AMOLED).apply()
                    }
                    Themes.GREEN -> {
                        e.putString(K.PREF.THEME, Themes.GREEN).apply()
                    }
                    Themes.SYSTEM_DEFAULT -> {
                        e.putString(K.PREF.THEME, Themes.SYSTEM_DEFAULT).apply()
                    }
                    Themes.WHITE -> {
                        e.putString(K.PREF.THEME, Themes.WHITE).apply()
                    }
                    Themes.DARK_GREEN -> {
                        e.putString(K.PREF.THEME, Themes.DARK_GREEN).apply()
                    }
                }

                Themes.changeToTheme(activity as AppCompatActivity?)

                if (userPrefs.getString(K.PREF.THEME, Themes.DARKNESS) == Themes.DARKNESS) {
                    if (context != null) {
                        Toast.makeText(context, "My old friend, I've come to talk with you again", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            if (key == K.PREF.USER_TYPE) {
                val e = userPrefs.edit()
                when (sharedPreferences.getString(key, "normal")) {
                    "normal" -> {
                        e.putInt(K.PREF.USER_TYPE, K.USER_TYPE_NORMAL)
                        findPreference<Preference>(K.PREF.EXTENDED_LOGGING_ENABLED)?.isVisible = false
                    }
                    "expert" -> {
                        e.putInt(K.PREF.USER_TYPE, K.USER_TYPE_EXPERT)
                        findPreference<Preference>(K.PREF.EXTENDED_LOGGING_ENABLED)?.isVisible = false
                    }
                    "chuck" -> {
                        e.putInt(K.PREF.USER_TYPE, K.USER_TYPE_CHUCK_NORRIS)
                        findPreference<Preference>(K.PREF.EXTENDED_LOGGING_ENABLED)?.isVisible = true
                    }
                    else -> {
                        e.putInt(K.PREF.USER_TYPE, K.USER_TYPE_NORMAL)
                        findPreference<Preference>(K.PREF.EXTENDED_LOGGING_ENABLED)?.isVisible = false
                    }
                }
                e.apply()
            }

            if (key == K.PREF.ENGLISH_LANGUAGE) {
                val e = userPrefs.edit()
                if (!sharedPreferences.getBoolean(key, false)) {
                    Toast.makeText(context, R.string.info_restart_app, Toast.LENGTH_LONG).show()
                    e.putBoolean(K.PREF.ENGLISH_LANGUAGE, false).apply()
                } else {
                    e.putBoolean(K.PREF.ENGLISH_LANGUAGE, true).apply()
                    activity?.let {
                        Utils.toEnglish(it)
                        startActivity(Intent(it, it::class.java))
                        it.finish()
                    }
                }
            }

            if (key == K.PREF.EXTENDED_LOGGING_ENABLED) {
                userPrefs.edit().putBoolean(key, sharedPreferences.getBoolean(key, false)).apply()
            }

        } catch (e: Exception) {
            Logger.logError(e, context)
        }

    }
}
