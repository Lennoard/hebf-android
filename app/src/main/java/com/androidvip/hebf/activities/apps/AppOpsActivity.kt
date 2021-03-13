package com.androidvip.hebf.activities.apps

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.R
import com.androidvip.hebf.activities.BaseActivity
import com.androidvip.hebf.models.App
import com.androidvip.hebf.toast
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.Logger
import com.androidvip.hebf.utils.RootUtils
import kotlinx.android.synthetic.main.activity_app_ops.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable

class AppOpsActivity : BaseActivity() {
    data class AppOp(
        val operationName: String,
        val operationDescription: String, var enabled: Boolean
    ) : Serializable

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_ops)

        setUpToolbar(toolbar)

        // Check the data from intent
        val app = intent.getSerializableExtra(K.EXTRA_APP) as App?
        if (app != null) {
            // Update Toolbar title with app name
            supportActionBar?.title = app.label
            supportActionBar?.subtitle = app.packageName

            lifecycleScope.launch {
                val appOps = getAppOps(app.packageName)
                setupRecyclerView(appOps, app.packageName)
            }
        } else {
            // Something went wrong, return back to the previous screen
            Logger.logWTF("Failed to show appops because no app was provided to begin with", this)
            toast(R.string.failed)
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView(appOps: List<AppOp>, packageName: String) {
        findViewById<RecyclerView>(R.id.app_ops_rv)?.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this@AppOpsActivity, defineRecyclerViewColumns())
            adapter = AppOpAdapter(this@AppOpsActivity, appOps, packageName)
        }
    }

    private fun defineRecyclerViewColumns(): Int {
        val isTablet = resources.getBoolean(R.bool.is_tablet)
        val isLandscape = resources.getBoolean(R.bool.is_landscape)
        return if (isTablet || isLandscape) 2 else 1
    }

    private suspend fun getAppOps(packageName: String): List<AppOp> = withContext(Dispatchers.Default) {
        val appOps = mutableListOf<AppOp>()

        Logger.logDebug(
            "Getting app operations for package '$packageName'",
            this@AppOpsActivity
        )
        RootUtils.executeWithOutput("appops get $packageName", "") { line ->
            runCatching {
                val splitOp = line.split(":")
                val permissionName = splitOp.first().trim()
                appOps.add(AppOp(permissionName, findDescriptionFromOp(permissionName), line.contains("allow")))
            }
        }

        return@withContext appOps
    }

    private fun findDescriptionFromOp(op: String): String {
        return mapOf(
            "COARSE_LOCATION" to "Allows an app to access approximate location",
            "FINE_LOCATION" to "Allows an application to access precise location",
            "ACCESS_FINE_LOCATION" to "Allows an application to access precise location",
            "GPS" to "Allows an application to access the GPS",
            "VIBRATE" to "Allows access to the vibrator",
            "READ_CONTACTS" to "Allows an application to read the user's contacts data",
            "WRITE_CONTACTS" to "Allows an application to write the user's contacts data",
            "READ_CALL_LOG" to "Allows an application to read the user's call log",
            "WRITE_CALL_LOG" to "Allows an application to write (but not read) the user's call log data",
            "READ_CALENDAR" to "Allows an application to read the user's calendar data",
            "WRITE_CALENDAR" to "Allows an application to write the user's calendar data",
            "WIFI_SCAN" to "Allows an application to access scan WiFi networks",
            "POST_NOTIFICATION" to "Allows an application to post notifications",
            "NEIGHBORING_CELLS" to "Allows an application to received Signal Strength and Cell ID location",
            "CALL_PHONE" to "Allows an application to initiate a phone call without going through the Dialer user interface for the user to confirm the call",
            "READ_SMS" to "Allows an application to read SMS messages",
            "WRITE_SMS" to "Allows an application to send SMS messages",
            "RECEIVE_SMS" to "Allows an application to receive SMS messages",
            "RECEIVE_EMERGENCY_SMS" to "Allows an application to receive Emergency SMS messages",
            "RECEIVE_MMS" to "Allows an application to monitor incoming MMS messages",
            "RECEIVE_WAP_PUSH" to "Allows an application to receive WAP push messages",
            "SEND_SMS" to "Allows an application to send SMS messages",
            "READ_ICC_SMS" to "",
            "WRITE_ICC_SMS" to "",
            "WRITE_SETTINGS" to "Read or write the system settings (there are limitations)",
            "SYSTEM_ALERT_WINDOW" to "Create windows and show them on top of all other apps",
            "ACCESS_NOTIFICATIONS" to "Allows a system app to access notifications",
            "CAMERA" to "Required to be able to access the camera device",
            "RECORD_AUDIO" to "Allows an application to record audio",
            "PLAY_AUDIO" to "",
            "READ_CLIPBOARD" to "Read contents of the clipboard service",
            "WRITE_CLIPBOARD" to "Write data to the clipboard",
            "TAKE_MEDIA_BUTTONS" to "",
            "TAKE_AUDIO_FOCUS" to "Allows an application to be the only one holding audio focus",
            "AUDIO_MASTER_VOLUME" to "",
            "AUDIO_VOICE_VOLUME" to "",
            "AUDIO_RING_VOLUME" to "Change ring volume",
            "AUDIO_MEDIA_VOLUME" to "",
            "AUDIO_ALARM_VOLUME" to "",
            "AUDIO_NOTIFICATION_VOLUME" to "Change notification volume",
            "AUDIO_BLUETOOTH_VOLUME" to "",
            "WAKE_LOCK" to "Use PowerManager WakeLocks to keep processor from sleeping or screen from dimming",
            "MONITOR_LOCATION" to "",
            "MONITOR_HIGH_POWER_LOCATION" to "",
            "GET_USAGE_STATS" to "Allows an application to collect component usage statistics",
            "PACKAGE_USAGE_STATS" to "Allows an application to collect component usage statistics",
            "MUTE_MICROPHONE" to "Allows an application to access mute the microphone",
            "TOAST_WINDOW" to "Show short duration messages on screen",
            "PROJECT_MEDIA" to "",
            "ACTIVATE_VPN" to "",
            "WRITE_WALLPAPER" to "Allows an application to set the wallpaper",
            "ASSIST_STRUCTURE" to "",
            "ASSIST_SCREENSHOT" to "",
            "OP_READ_PHONE_STATE" to "Allows read only access to phone state, including the phone number of the device, current cellular network information, the status of any ongoing calls, and a list of any phone accounts registered on the device",
            "ADD_VOICEMAIL" to "",
            "USE_SIP" to "",
            "PROCESS_OUTGOING_CALLS" to "Allows an application to see the number being dialed during an outgoing call with the option to redirect the call to a different number or abort the call altogether",
            "USE_FINGERPRINT" to "Allows an app to use fingerprint hardware",
            "BODY_SENSORS" to "Access data from sensors that the user uses to measure what is happening inside his/her body, such as heart rate",
            "READ_CELL_BROADCASTS" to "",
            "MOCK_LOCATION" to "",
            "READ_EXTERNAL_STORAGE" to "Allows an application to read from external storage",
            "WRITE_EXTERNAL_STORAGE" to "Allows an application to write to external storage",
            "TURN_ON_SCREEN" to "",
            "GET_ACCOUNTS" to "Allows access to the list of accounts in the Accounts Service",
            "RUN_IN_BACKGROUND" to "Allows an application to run in the background",
            "AUDIO_ACCESSIBILITY_VOLUME" to "",
            "READ_PHONE_NUMBERS" to "Allows read access to the device's phone number(s). This is a subset of the capabilities granted by READ_PHONE_STATE but is exposed to instant applications",
            "REQUEST_INSTALL_PACKAGES" to "Allows an application to install packages. Not for use by third-party applications",
            "PICTURE_IN_PICTURE" to "",
            "INSTANT_APP_START_FOREGROUND" to "",
            "ANSWER_PHONE_CALLS" to "",
            "RUN_ANY_IN_BACKGROUND" to "",
            "CHANGE_WIFI_STATE" to "Allows applications to change Wi-Fi connectivity state",
            "REQUEST_DELETE_PACKAGES" to "Allows an application to request deleting packages",
            "BIND_ACCESSIBILITY_SERVICE" to "Required by an Accessibility Service, to ensure that only the system can bind to it",
            "ACCEPT_HANDOVER" to "",
            "MANAGE_IPSEC_TUNNELS" to "",
            "START_FOREGROUND" to "Start foreground services. Requires FOREGROUND_SERVICE permission",
            "BLUETOOTH_SCAN" to "",
            "USE_BIOMETRIC" to "Use device supported biometric modalities",
            "BOOT_COMPLETED" to "Receive a broadcast indicating that the system has finished booting"
        ).getOrEmpty(op)
    }

    private fun Map<String, String>.getOrEmpty(key: String): String {
        return if (this.containsKey(key)) this.getValue(key) else ""
    }

    class AppOpAdapter(
        private val activity: BaseActivity,
        private val appOps: List<AppOp>,
        private val packageName: String
    ) : RecyclerView.Adapter<AppOpAdapter.ViewHolder>() {

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            var name: TextView = v.findViewById(R.id.app_ops_name)
            var description: TextView = v.findViewById(R.id.app_ops_description)
            var state: SwitchCompat = v.findViewById(R.id.app_ops_switch)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(activity).inflate(R.layout.list_item_app_ops_switch, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val appOp = appOps[position]

            holder.name.text = appOp.operationName
            holder.description.text = appOp.operationDescription

            holder.state.setOnCheckedChangeListener(null)
            holder.state.isChecked = appOp.enabled
            holder.state.setOnCheckedChangeListener { _, isChecked -> setAppOp(appOp, isChecked) }
        }

        override fun getItemCount(): Int {
            return appOps.size
        }

        private fun setAppOp(appOp: AppOp, enable: Boolean) {
            val state = if (enable) "allow" else "deny"
            Logger.logInfo("Setting ${appOp.operationName} of $packageName to '$state'", activity)
            activity.runCommand("appops set $packageName ${appOp.operationName} $state")
        }
    }

}
