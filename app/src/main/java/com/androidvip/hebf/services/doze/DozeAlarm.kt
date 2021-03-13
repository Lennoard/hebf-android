package com.androidvip.hebf.services.doze

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.androidvip.hebf.utils.Doze
import com.androidvip.hebf.utils.Prefs
import java.util.*

@RequiresApi(api = Build.VERSION_CODES.M)
class DozeAlarm : BroadcastReceiver() {
    private var listPrefKey = "doze_schedule_list_"

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = Prefs(context)

        intent.getStringExtra(Doze.EXTRA_ALARM_ACTION)?.let { extra ->
            if (extra == Doze.ALARM_ACTION_BLACKLIST) {
                val listType = intent.getStringExtra("list_type")
                listPrefKey += listType
                val set = prefs.getStringSet(listPrefKey, HashSet())

                if (listType == Doze.LIST_TYPE_WHITELIST)
                    Doze.blacklist(set) // Blacklist apps currently on whitelist
                else
                    Doze.whitelist(set)
            }
        }
    }
}
