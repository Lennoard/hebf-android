package com.androidvip.hebf.utils.ext

import android.app.AppOpsManager
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.PixelFormat
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.androidvip.hebf.*
import com.androidvip.hebf.utils.Logger.logError
import com.androidvip.hebf.utils.Logger.logWTF
import com.androidvip.hebf.utils.UserPrefs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.ShellUtils
import java.util.*

fun Context?.toast(messageRes: Int, short: Boolean = true) {
    if (this == null) return
    toast(getString(messageRes), short)
}

fun Context?.toast(message: String?, short: Boolean = true) {
    if (message == null || this == null) return
    val length = if (short) Toast.LENGTH_SHORT else Toast.LENGTH_LONG

    val ctx = this
    if (ShellUtils.onMainThread()) {
        Toast.makeText(ctx, message, length).show()
    } else {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(ctx, message, length).show()
        }
    }
}

inline fun Context.confirm(
    message: String = getString(R.string.confirmation_message),
    crossinline onConfirm: () -> Unit
) = MaterialAlertDialogBuilder(this).apply {
    setTitle(android.R.string.dialog_alert_title)
    setMessage(message)
    setCancelable(false)
    setNegativeButton(R.string.cancelar) { _, _ -> }
    setPositiveButton(android.R.string.yes) { _, _ ->
        onConfirm()
    }
    applyAnim().also {
        it.show()
    }
}

fun Context.runOnMainThread(f: Context.() -> Unit) {
    if (Looper.getMainLooper() === Looper.myLooper()) {
        f()
    } else {
        Handler(Looper.getMainLooper()).post { f() }
    }
}

fun Context.createVectorDrawable(@DrawableRes resId: Int ) : VectorDrawableCompat? {
    return VectorDrawableCompat.create(resources, resId, theme)
}

fun Fragment.createVectorDrawable(@DrawableRes resId: Int ) : VectorDrawableCompat? {
    return requireContext().createVectorDrawable(resId)
}

fun Context.getThemedVectorDrawable(@DrawableRes resId: Int): VectorDrawableCompat? {
    return createVectorDrawable(resId)?.apply {
        setTintCompat(getColorFromAttr(R.attr.colorOnSurface))
    }
}

fun Context?.hasUsageStatsPermission(): Boolean {
    if (this == null) return false
    return try {
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val mode = appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    applicationInfo.uid,
                    applicationInfo.packageName
                )
                mode == AppOpsManager.MODE_ALLOWED
            } else false
        } else false
    } catch (e: Exception) {
        false
    }
}

/**
 * Checks if the devices is connected to a network and
 * this network has connections stabilised
 *
 * @return true if the device is online, false otherwise
 */
fun Context?.isOnline(): Boolean {
    if (this == null) return false
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetworkInfo
    return activeNetwork != null && activeNetwork.isConnectedOrConnecting
}

/**
 * Shows an web page in a dialog box. The dialog allows refreshing
 * the current page via [SwipeRefreshLayout]
 *
 * @param url the url to load into the dialog's WebView
 */
fun Context?.webDialog(url: String?) {
    if (this == null || url.isNullOrEmpty()) return

    val dialog = Dialog(this).apply {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_web)
        setCancelable(true)
    }

    val webView = dialog.findViewById<WebView>(R.id.webView)
    val pb = dialog.findViewById<ProgressBar>(R.id.pb_home)
    val swipeLayout: SwipeRefreshLayout = dialog.findViewById(R.id.swipeToRefresh)
    swipeLayout.setColorSchemeResources(R.color.colorAccent)
    swipeLayout.setOnRefreshListener { webView.reload() }

    webView.apply {
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        loadUrl(url)
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                swipeLayout.isRefreshing = false
            }
        }
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                pb.progress = progress
                if (progress == 100) {
                    pb.visibility = View.GONE
                    swipeLayout.isRefreshing = false
                } else {
                    pb.visibility = View.VISIBLE
                }
            }
        }
    }
    dialog.show()
}

/**
 * Opens a web page in a external app
 *
 * @param context used to start the new activity
 * @param url the url to load externally
 */
fun Context?.launchUrl(url: String?) {
    if (this == null) return
    try {
        val uri = Uri.parse(url)
        val i = Intent(Intent.ACTION_VIEW, uri)
        startActivity(i)
    } catch (e: ActivityNotFoundException) {
        logWTF("Could not start browser: ${e.message}", this)
    }
}

/**
 * Changes the interface text to English (US)
 */
fun Context?.toEnglish() {
    if (this == null) return
    val userPrefs = UserPrefs(applicationContext)
    val englishLanguage = userPrefs.getBoolean("english_language", false)
    if (englishLanguage) {
        val locale = Locale("en")
        Locale.setDefault(locale)
        val config = Configuration()
        config.locale = locale
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}

fun Context?.hasStoragePermissions(): Boolean {
    if (this == null) return false

    val perm = "android.permission.READ_EXTERNAL_STORAGE"
    val res = checkCallingOrSelfPermission(perm)
    return res == PackageManager.PERMISSION_GRANTED
}

fun Context?.canDrawOverlays(): Boolean {
    if (this == null) return false
    return when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> true
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 -> {
            Settings.canDrawOverlays(this)
        }
        else -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) return true
            // Android 8.0 Oreo bug, always returns false
            // This workaround tries to create a window and if an exception is thrown then return false
            try {
                val mgr = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return false
                val viewToAdd = View(this)
                val params = WindowManager.LayoutParams(
                    0,
                    0,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        WindowManager.LayoutParams.TYPE_PHONE
                    },
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSPARENT
                )
                viewToAdd.layoutParams = params
                mgr.addView(viewToAdd, params)
                mgr.removeView(viewToAdd)
                return true
            } catch (e: java.lang.Exception) {
                logError(e, applicationContext)
            }
            false
        }
    }
}

fun Context?.requestWriteSettingsPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || this == null) return
    if (!Settings.System.canWrite(this)) {
        val perm = MaterialAlertDialogBuilder(this)
        perm.setTitle(R.string.app_name)
        perm.setMessage(getString(R.string.mod_settings_dialog))
        perm.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
        perm.setPositiveButton(android.R.string.ok) { _, _ ->
            try {
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(this)
                }
            } catch (e: java.lang.Exception) {
                logWTF(e.message, applicationContext)
            }
        }
        perm.create().also {
            runCatching { it.show() }
        }
    }
}