package com.androidvip.hebf.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.androidvip.hebf.utils.GameBooster
import com.androidvip.hebf.utils.Logger
import com.androidvip.hebf.utils.VipBatterySaver
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@ExperimentalContracts
class TaskerReceiver : BroadcastReceiver() {

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

    override fun onReceive(context: Context, intent: Intent) {
        if (ACTION_FIRE_SETTING != intent.action) return

        val bundle = intent.getBundleExtra(EXTRA_BUNDLE)
        if (bundle.isValidTaskerBundle()) {
            val actionType = bundle.getInt(BUNDLE_EXTRA_ACTION_TYPE, ACTION_TYPE_INVALID)
            when (bundle.getInt(BUNDLE_EXTRA_HEBF_FEATURE, HEBF_FEATURE_INVALID)) {
                HEBF_FEATURE_VIP -> {
                    when (actionType) {
                        ACTION_TYPE_ENABLE -> VipBatterySaver.toggle(true, context.applicationContext)
                        ACTION_TYPE_DISABLE -> VipBatterySaver.toggle(false, context.applicationContext)
                    }
                }
                HEBF_FEATURE_GB -> {
                    when (actionType) {
                        ACTION_TYPE_ENABLE -> GameBooster.toggle(true, context.applicationContext)
                        ACTION_TYPE_DISABLE -> GameBooster.toggle(false, context.applicationContext)
                    }
                }
            }
        } else {
            Logger.logWarning("Invalid tasker bundle: $bundle", context)
        }
    }

}

@ExperimentalContracts
fun Bundle?.isValidTaskerBundle() : Boolean {
    contract {
        returns(true) implies (this@isValidTaskerBundle != null)
    }

    return this != null
        && containsKey(TaskerReceiver.BUNDLE_EXTRA_HEBF_FEATURE)
        && containsKey(TaskerReceiver.BUNDLE_EXTRA_ACTION_TYPE)
}
