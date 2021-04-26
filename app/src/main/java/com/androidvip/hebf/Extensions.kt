package com.androidvip.hebf

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.SparseArray
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.*
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.Fragment
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.androidvip.hebf.ui.base.BaseFragment
import com.androidvip.hebf.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.topjohnwu.superuser.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URL
import java.util.*

typealias SnackbarDuration = BaseTransientBottomBar.Duration

fun View.goAway() {
    this.visibility = View.GONE
}

fun View.hide() {
    this.visibility = View.INVISIBLE
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun ProgressBar.animProgress(progress: Int) {
    ObjectAnimator.ofInt(
        this, "progress", this.progress, progress
    ).apply {
        duration = 400
        interpolator = DecelerateInterpolator()
        start()
    }
}

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

fun View.snackbar(
        @StringRes stringRes: Int,
        @SnackbarDuration duration: Int = Snackbar.LENGTH_SHORT
) {
    snackbar(context.getString(stringRes), duration)
}

fun View.snackbar(msg: String, @SnackbarDuration duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(this, msg, duration).show()
}

inline fun Context.confirm(
        message: String = getString(R.string.confirmation_message),
        crossinline onConfirm: () -> Unit
) = MaterialAlertDialogBuilder(this).apply {
    setTitle(android.R.string.dialog_alert_title)
    setIcon(getThemedVectorDrawable(R.drawable.ic_warning))
    setMessage(message)
    setCancelable(false)
    setNegativeButton(R.string.cancelar) { _, _ -> }
    setPositiveButton(android.R.string.yes) { _, _ ->
        onConfirm()
    }
    show()
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

fun Drawable.setTintCompat(color: Int, blendModeCompat: BlendModeCompat = BlendModeCompat.SRC_ATOP) {
    colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            color, blendModeCompat
    )
}

@ColorInt
fun Context.getColorFromAttr(
        @AttrRes attrColor: Int,
        typedValue: TypedValue = TypedValue(),
        resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

fun Context.getAccentColor() = getColorFromAttr(R.attr.colorAccent)

fun Int.toPx(context: Context): Int {
    if (Utils.isInvalidContext(context)) return this
    var px = this
    runCatching {
        px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics).toInt()
    }
    return px
}

fun Double.roundTo2Decimals(decimals: Int): Double {
    return BigDecimal(this).setScale(decimals, RoundingMode.HALF_UP).toDouble()
}

infix fun Number.percentOf(other: Number): Double {
    return (this.toDouble() / 100) * other.toDouble()
}
infix fun Number.isWhatPercentOf(other: Number): Double {
    return (this.toDouble() / other.toDouble()) * 100
}

infix fun Int.isMultipleOf(target: Int): Boolean = this % target == 0

fun Int.findNearestPositiveMultipleOf(target: Int): Int {
    if (this isMultipleOf target) return this

    var testUp = this
    var testDown = this

    while (true) {
        if (testUp isMultipleOf target) break
        testUp++
    }

    while (true) {
        if (testDown isMultipleOf target) break
        testDown--
    }

    if (testDown <= 0) return testUp

    if (testUp - target > target - testDown) return testDown
    return testUp
}

fun String.lower() = this.toLowerCase(Locale.getDefault())

suspend fun String.getBitMapFromUrl() : Bitmap? {
    val url = this
    return withContext(Dispatchers.IO) {
        runCatching {
            BitmapFactory.decodeStream(URL(url).openConnection().getInputStream())
        }.getOrNull()
    }
}

fun MaterialAlertDialogBuilder.applyAnim(
    @StyleRes styleResId: Int = R.style.AppDialogAnimation
): AlertDialog = create().also {
    it.window?.attributes?.windowAnimations = styleResId
}

fun Uri.readLines(context: Context?, forEachLine: (String) -> Unit) {
    context?.contentResolver?.openInputStream(this).readLines(forEachLine)
}

fun InputStream?.readLines(forEachLine: (String) -> Unit) {
    this?.use { inputStream ->
        inputStream.bufferedReader().use {
            it.readLines().forEach { line ->
                forEachLine(line)
            }
        }
    }
}


suspend inline fun Activity?.runSafeOnUiThread(crossinline uiBlock: () -> Unit) {
    if (this != null && !isFinishing) {
        withContext(Dispatchers.Main) {
            runCatching(uiBlock)
        }
    }
}

suspend inline fun BaseFragment.runSafeOnUiThread(crossinline uiBlock: () -> Unit) {
    if (isActivityAlive) {
        activity.runSafeOnUiThread(uiBlock)
    }
}

fun <T> sparseArrayOf(vararg pairs: Pair<Int, T>) : SparseArray<T> {
    val sparseArray = SparseArray<T>()
    pairs.forEach {
        sparseArray.put(it.first, it.second)
    }

    return sparseArray
}

inline fun <T> SparseArray<T>.forEach(action: (T) -> Unit) {
    for (i in 0 until this.size()) action(valueAt(i))
}

inline fun <T> SparseArray<T>.forEachIndexed(action: (index: Int, T) -> Unit) {
    var index = 0
    for (i in 0 until this.size()) action(index++, valueAt(i))
}