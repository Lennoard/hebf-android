package com.androidvip.hebf.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.androidvip.hebf.utils.Logger
import com.androidvip.hebf.utils.gb.GameBoosterImpl
import com.androidvip.hebf.utils.gb.GameBoosterNutellaImpl
import com.androidvip.hebf.utils.vip.VipBatterySaverImpl
import com.androidvip.hebf.utils.vip.VipBatterySaverNutellaImpl
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TaskerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ACTION_FIRE_SETTING != intent.action) return

        val isRooted = Shell.rootAccess()
        val gameBooster = if (isRooted) {
            GameBoosterImpl(context)
        } else {
            GameBoosterNutellaImpl(context)
        }

        val vip = if (isRooted) {
            VipBatterySaverImpl(context)
        } else {
            VipBatterySaverNutellaImpl(context)
        }

        val bundle = intent.getBundleExtra(EXTRA_BUNDLE)
        if (isBundleValid(bundle)) {
            val actionType = bundle.getInt(BUNDLE_EXTRA_ACTION_TYPE, ACTION_TYPE_INVALID)
            when (bundle.getInt(BUNDLE_EXTRA_HEBF_FEATURE, HEBF_FEATURE_INVALID)) {
                HEBF_FEATURE_VIP -> GlobalScope.launch {
                    when (actionType) {
                        ACTION_TYPE_ENABLE -> vip.enable()
                        ACTION_TYPE_DISABLE -> vip.disable()
                    }
                }
                HEBF_FEATURE_GB -> GlobalScope.launch {
                    when (actionType) {
                        ACTION_TYPE_ENABLE -> gameBooster.enable()
                        ACTION_TYPE_DISABLE -> gameBooster.disable()
                    }
                }
            }
        } else {
            Logger.logWarning("Invalid tasker bundle: $bundle", context)
        }
    }

    private fun isBundleValid(bundle: Bundle?): Boolean {
        return (bundle != null && bundle.containsKey(BUNDLE_EXTRA_HEBF_FEATURE) && bundle.containsKey(
            BUNDLE_EXTRA_ACTION_TYPE
        ))
    }

    companion object {
        const val EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE"
        const val EXTRA_STRING_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB"
        const val ACTION_FIRE_SETTING = "com.twofortyfouram.locale.intent.action.FIRE_SETTING"

        const val BUNDLE_EXTRA_HEBF_FEATURE = "com.androidvip.hebf.tasker.extra.HEBF_FEATURE"
        const val BUNDLE_EXTRA_ACTION_TYPE = "com.androidvip.hebf.tasker.extra.ACTION_TYPE"

        const val HEBF_FEATURE_INVALID: Int = -1
        const val HEBF_FEATURE_GB: Int = 0
        const val HEBF_FEATURE_VIP: Int = 1

        const val ACTION_TYPE_INVALID: Int = -1
        const val ACTION_TYPE_DISABLE: Int = 0
        const val ACTION_TYPE_ENABLE: Int = 1
    }

}
