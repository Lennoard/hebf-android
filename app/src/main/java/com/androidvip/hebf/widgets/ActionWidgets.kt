package com.androidvip.hebf.widgets

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.R
import com.androidvip.hebf.activities.BaseActivity
import com.androidvip.hebf.utils.*
import com.androidvip.hebf.utils.Utils.dpToPx
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

private fun generateLogView(context: Context): ViewGroup {
    val rootView = LinearLayout(context)
    rootView.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
    rootView.orientation = LinearLayout.VERTICAL

    val logHolder = TextView(context)
    logHolder.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
    logHolder.setPadding(
        dpToPx(context, 16), dpToPx(context, 16),
        dpToPx(context, 16), dpToPx(context, 16)
    )
    logHolder.id = R.id.log_holder
    logHolder.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
    rootView.addView(logHolder)

    return rootView
}

private inline fun toggleServices(activityRef: WeakReference<Activity>, block: () -> Unit) {
    val rooted = UserPrefs(activityRef.get()!!).getBoolean(K.PREF.USER_HAS_ROOT, true)
    if (rooted) {
        block()
    }
    activityRef.get()?.finish()
}

class WidgetFstrim : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(generateLogView(this))

        val logHolder = findViewById<TextView>(R.id.log_holder)
        logHolder.setText(R.string.enabling)

        lifecycleScope.launch(workerContext) {
            val result = if (Shell.rootAccess()) {
                RootUtils.executeWithOutput("busybox fstrim -v /data && busybox fstrim /cache", "", this@WidgetFstrim)
            } else {
                "Only for rooted users!"
            }

            runSafeOnUiThread {
                logHolder.text = result
            }
        }
    }
}

class WidgetBoost : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(generateLogView(this))

        lifecycleScope.launch(workerContext) {
            RootUtils.executeSync("sync && sysctl -w vm.drop_caches=3")

            runSafeOnUiThread {
                Toast.makeText(this@WidgetBoost, "Cache cleaned", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}

class WidgetVipBatterySaver : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = intent.extras
        if (extras != null) {
            val turnOn = extras.getBoolean("ativar")

            if (turnOn) {
                Toast.makeText(this, getString(R.string.vip_battery_saver_on), Toast.LENGTH_SHORT).show()
                toggleServices(WeakReference(this)) {
                    VipBatterySaver.toggle(true, applicationContext)
                }
            } else {
                Toast.makeText(this, getString(R.string.vip_battery_saver_off), Toast.LENGTH_SHORT).show()
                toggleServices(WeakReference(this)) {
                    VipBatterySaver.toggle(false, this)
                    getSharedPreferences("VIP", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(K.PREF.VIP_SHOULD_STILL_ACTIVATE, false)
                        .apply()
                }
            }
        } else {
            // FIX: VipWidgetProvider
            finish()
        }
    }
}

class WidgetGameBooster : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val extras = intent.extras

        if (extras != null) {
            val turnOn = extras.getBoolean("ativar")

            if (turnOn) {
                Toast.makeText(this, "Boosting...", Toast.LENGTH_SHORT).show()
                toggleServices(WeakReference(this)) {
                    GameBooster.toggle(true, applicationContext)
                }
            } else {
                Toast.makeText(this, "Disabling Game Booster", Toast.LENGTH_SHORT).show()
                toggleServices(WeakReference(this)) {
                    GameBooster.toggle(false, applicationContext)
                }
            }
        } else {
            // FIX: GameBoosterWidgetProvider
            finish()
        }
    }

}
