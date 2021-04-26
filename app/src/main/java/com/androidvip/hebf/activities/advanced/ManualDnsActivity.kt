package com.androidvip.hebf.activities.advanced

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.text.InputFilter
import android.text.Spanned
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.R
import com.androidvip.hebf.helpers.HebfApp
import com.androidvip.hebf.ui.base.BaseActivity
import com.androidvip.hebf.utils.*
import com.androidvip.hebf.utils.K.PREF.DNS_SPINNER_SELECTION
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_manual_dns.*
import kotlinx.coroutines.launch
import java.util.*

class ManualDnsActivity : BaseActivity() {
    private lateinit var dns1Fields: Array<EditText>
    private lateinit var dns2Fields: Array<EditText>
    private lateinit var dnsPresets: Array<String>
    private var validDns1: Boolean = false
    private var validDns2: Boolean = false
    private var firstOpen = true
    private val handler = Handler()
    private lateinit var checkInputsRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_dns)

        setUpToolbar(toolbar)

        setFilters()
        setUpRunnable()

        dnsApplyButton.hide()

        dnsPresets = arrayOf(
            getString(R.string.default_),
            "Google DNS", "Cloudflare DNS", "Open DNS",
            "Level3 Public DNS", "Verisign Public DNS",
            "Comodo Secure DNS", "DNS.WATCH", "Dyn DNS"
        )

        Logger.logDebug("Reading current DNS", this)
        lifecycleScope.launch(workerContext) {
            val currentDns1 = Utils.getProp("net.wlan0.dns1", "8.8.8.8")
            val currentDns2 = Utils.getProp("net.wlan0.dns1", "8.8.4.4")

            runSafeOnUiThread {
                val adapter = ArrayAdapter(
                    this@ManualDnsActivity, android.R.layout.simple_spinner_item, dnsPresets
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                dnsPresetsSpinner.adapter = adapter
                dnsPresetsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        prefs.putInt(DNS_SPINNER_SELECTION, position)
                        when (position) {
                            0 -> {
                                if (!firstOpen) {
                                    populateTextFields(prefs.getString(K.PREF.DNS_1, currentDns1), prefs.getString(K.PREF.DNS_2, currentDns2))
                                }
                            }
                            1 -> {
                                populateTextFields("8.8.8.8", "8.8.4.4")
                                setDnsInfoListener("https://developers.google.com/speed/public-dns/")
                            }
                            2 -> {
                                populateTextFields("1.1.1.1", "1.0.0.1")
                                setDnsInfoListener("https://1.1.1.1/")
                            }
                            3 -> {
                                populateTextFields("208.67.222.222", "208.67.220.220")
                                setDnsInfoListener("https://www.opendns.com/")
                            }
                            4 -> {
                                populateTextFields("209.244.0.3", "209.244.0.4")
                                setDnsInfoListener("http://www.level3.com/en/")
                            }
                            5 -> {
                                populateTextFields("64.6.64.6", "64.6.65.6")
                                setDnsInfoListener("https://www.verisign.com/en_US/security-services/public-dns/index.xhtml")
                            }
                            6 -> {
                                populateTextFields("8.26.56.26", "8.20.247.20")
                                setDnsInfoListener("https://www.comodo.com/secure-dns/")
                            }
                            7 -> {
                                populateTextFields("84.200.69.80", "84.200.70.40")
                                setDnsInfoListener("https://dns.watch/")
                            }
                            8 -> {
                                populateTextFields("216.146.35.35", "216.146.36.36")
                                setDnsInfoListener("https://help.dyn.com/standard-dns/")
                            }
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {

                    }
                }

                populateTextFields(currentDns1, currentDns2)

                when (currentDns1) {
                    "8.8.8.8" -> {
                        setDnsInfoListener("https://developers.google.com/speed/public-dns/")
                        dnsPresetsSpinner.setSelection(1)
                    }
                    "1.1.1.1" -> {
                        setDnsInfoListener("https://1.1.1.1/")
                        dnsPresetsSpinner.setSelection(2)
                    }
                    "208.67.222.222" -> {
                        setDnsInfoListener("https://www.opendns.com/")
                        dnsPresetsSpinner.setSelection(3)
                    }
                    "209.244.0.3" -> {
                        setDnsInfoListener("http://www.level3.com/en/")
                        dnsPresetsSpinner.setSelection(4)
                    }
                    "64.6.64.6" -> {
                        setDnsInfoListener("https://www.verisign.com/en_US/security-services/public-dns/index.xhtml")
                        dnsPresetsSpinner.setSelection(5)
                    }
                    "8.26.56.26" -> {
                        setDnsInfoListener("https://www.comodo.com/secure-dns/")
                        dnsPresetsSpinner.setSelection(6)
                    }
                    "84.200.69.80" -> {
                        setDnsInfoListener("https://dns.watch/")
                        dnsPresetsSpinner.setSelection(7)
                    }
                    "216.146.35.35" -> {
                        setDnsInfoListener("https://help.dyn.com/standard-dns/")
                        dnsPresetsSpinner.setSelection(8)
                    }
                    else -> dnsPresetsSpinner.setSelection(0)
                }
            }
        }

        dnsInfoButton.setOnClickListener {
            val userPrefs = UserPrefs(applicationContext)
            val achievementsSet = userPrefs.getStringSet(K.PREF.ACHIEVEMENT_SET, HashSet())
            if (!achievementsSet.contains("help")) {
                Utils.addAchievement(applicationContext, "help")
                Toast.makeText(this, getString(R.string.achievement_unlocked, getString(R.string.achievement_help)), Toast.LENGTH_LONG).show()
            }
            val dialog = Dialog(this).apply {
                setContentView(R.layout.dialog_log)
                setTitle("DNS info")
                setCancelable(true)
            }

            lifecycleScope.launch(workerContext) {
                val props = RootUtils.executeWithOutput("getprop | grep .dns")

                runSafeOnUiThread {
                    try {
                        val holder = dialog.findViewById<TextView>(R.id.log_holder)
                        holder.text = props
                    } catch (e: Exception) {
                        Logger.logError(e, this@ManualDnsActivity)
                    }
                }
            }

            dialog.show()
        }

        dnsApplyOnBoot.setOnCheckedChangeListener(null)
        dnsApplyOnBoot.isChecked = prefs.getBoolean(K.PREF.DNS_ON_BOOT, false)
        dnsApplyOnBoot.setOnCheckedChangeListener { _, isChecked ->
            prefs.putBoolean(K.PREF.DNS_ON_BOOT, isChecked)
        }

        dnsApplyButton.setOnClickListener {
            if (prefs.getBoolean(K.PREF.NET_GOOGLE_DNS, false)) {
                AlertDialog.Builder(this@ManualDnsActivity)
                    .setTitle(getString(R.string.title_activity_dns))
                    .setMessage("This will override the \"Use Google DNS\" option")
                    .setNegativeButton(R.string.cancelar) { _, _ -> }
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        Snackbar.make(dnsApplyButton, R.string.value_set, Snackbar.LENGTH_LONG).show()
                    }
                    .show()
            } else {
                applyDns()
            }
        }

    }

    override fun onStart() {
        super.onStart()

        handler.removeCallbacks(checkInputsRunnable)
        handler.postDelayed(checkInputsRunnable, 250)
    }

    override fun onStop() {
        super.onStop()

        handler.removeCallbacks(checkInputsRunnable)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fragment_close_enter, android.R.anim.slide_out_right)
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setUpRunnable() {
        checkInputsRunnable = object : Runnable {
            override fun run() {
                checkDns()
                handler.postDelayed(this, 1000)
            }
        }
    }

    private fun setDnsInfoListener(url: String) {
        dnsUrlButton.setOnClickListener {
            val userPrefs = UserPrefs(applicationContext)
            val achievementsSet = userPrefs.getStringSet(K.PREF.ACHIEVEMENT_SET, HashSet())
            if (!achievementsSet.contains("help")) {
                Utils.addAchievement(applicationContext, "help")
                Toast.makeText(this, getString(R.string.achievement_unlocked, getString(R.string.achievement_help)), Toast.LENGTH_LONG).show()
            }
            Utils.webDialog(this@ManualDnsActivity, url)
        }
    }

    private fun populateTextFields(dns1: String, dns2: String) {
        firstOpen = false
        val octets1 = dns1.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val octets2 = dns2.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        try {
            for (i in dns1Fields.indices) {
                dns1Fields[i].setText(octets1[i])
                dns1Fields[i].hint = octets1[i]
            }
            for (i in dns2Fields.indices) {
                dns2Fields[i].setText(octets2[i])
                dns2Fields[i].hint = octets2[i]
            }
        } catch (e: Exception) {
            dns1Fields.forEach {
                it.hint = "254"
            }
        }
    }

    private fun checkDns() {
        lifecycleScope.launch(workerContext) {
            validDns1 = true
            validDns2 = true
            for (field in dns1Fields)
                if (field.text.toString() == "")
                    validDns1 = false

            for (field in dns2Fields)
                if (field.text.toString() == "")
                    validDns2 = false

            runSafeOnUiThread {
                if (validDns1) {
                    dns1Checker.setImageResource(R.drawable.ic_check)
                    dns1Checker.setColorFilter(ContextCompat.getColor(this@ManualDnsActivity, R.color.colorSuccess))
                } else {
                    dns1Checker.setImageResource(R.drawable.ic_close)
                    dns1Checker.setColorFilter(ContextCompat.getColor(this@ManualDnsActivity, R.color.colorError))
                }
                if (validDns2) {
                    dns2Checker.setImageResource(R.drawable.ic_check)
                    dns2Checker.setColorFilter(ContextCompat.getColor(this@ManualDnsActivity, R.color.colorSuccess))
                } else {
                    dns2Checker.setImageResource(R.drawable.ic_close)
                    dns2Checker.setColorFilter(ContextCompat.getColor(this@ManualDnsActivity, R.color.colorError))
                }
                if (validDns1 && validDns2)
                    dnsApplyButton.show()
                else
                    dnsApplyButton.hide()
            }
        }
    }

    private fun applyDns() {
        firstOpen = false
        val dns1Builder = StringBuilder()
        val dns2Builder = StringBuilder()
        dns1Fields.forEach {
            dns1Builder.append(it.text.toString()).append(".")
        }

        dns2Fields.forEach {
            dns2Builder.append(it.text.toString()).append(".")
        }

        // Remove last "." from the string
        val dns1 = dns1Builder.toString().removeSuffix(".")
        val dns2 = dns2Builder.toString().removeSuffix(".")

        lifecycleScope.launch(workerContext) {
            RootUtils.execute(arrayOf(
                "setprop net.dns1 $dns1",
                "setprop net.dns2 $dns2",
                "setprop net.rmnet0.dns1 $dns1",
                "setprop net.rmnet0.dns2 $dns2",
                "setprop net.gprs.dns1 $dns1",
                "setprop net.gprs.dns2 $dns2",
                "setprop net.ppp0.dns1 $dns1",
                "setprop net.ppp0.dns2 $dns2",
                "setprop net.wlan0.dns1 $dns1",
                "setprop net.wlan0.dns2 $dns2",
                "iptables -t nat -I OUTPUT -p udp --dport 53 -j DNAT --to-destination $dns1",
                "iptables -t nat -I OUTPUT -p tcp --dport 53 -j DNAT --to-destination $dns1"
            ))
        }


        if (dnsApplyOnBoot.isChecked) {
            prefs.putBoolean(K.PREF.DNS_ON_BOOT, true)
        }

        prefs.edit {
            putString(K.PREF.DNS_1, dns1)
            putString(K.PREF.DNS_2, dns2)
            putInt(DNS_SPINNER_SELECTION, dnsPresetsSpinner.selectedItemPosition)
        }

        Snackbar.make(dnsApplyButton, "DNS set to: $dns1 and $dns2", Snackbar.LENGTH_LONG).show()
        Logger.logInfo("DNS set to: $dns1 and $dns2", this)
    }

    private fun setFilters() {
        dns1_1.filters = arrayOf<InputFilter>(InputFilterMinMax(1, 254))
        dns1_2.filters = arrayOf<InputFilter>(InputFilterMinMax(0, 254))
        dns1_3.filters = arrayOf<InputFilter>(InputFilterMinMax(0, 255))
        dns1_4.filters = arrayOf<InputFilter>(InputFilterMinMax(1, 255))

        dns2_1.filters = arrayOf<InputFilter>(InputFilterMinMax(1, 254))
        dns2_2.filters = arrayOf<InputFilter>(InputFilterMinMax(0, 254))
        dns2_3.filters = arrayOf<InputFilter>(InputFilterMinMax(0, 255))
        dns2_4.filters = arrayOf<InputFilter>(InputFilterMinMax(1, 255))

        dns1Fields = arrayOf(dns1_1, dns1_2, dns1_3, dns1_4)
        dns2Fields = arrayOf(dns2_1, dns2_2, dns2_3, dns2_4)
    }

    private inner class InputFilterMinMax(val min: Int, val max: Int) : InputFilter {

        override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
            try {
                val input = Integer.parseInt(dest.toString() + source.toString())
                if (isInRange(min, max, input))
                    return null
            } catch (ignored: NumberFormatException) {
            }

            return ""
        }

        private fun isInRange(a: Int, b: Int, c: Int): Boolean {
            return if (b > a) c in a..b else c in b..a
        }
    }
}