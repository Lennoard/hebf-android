package com.androidvip.hebf.ui.main.tune.cpu

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.StringDef
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.*
import com.androidvip.hebf.ui.base.BaseFragment
import com.androidvip.hebf.helpers.CPUDatabases
import com.androidvip.hebf.helpers.HebfApp
import com.androidvip.hebf.ui.base.BaseActivity
import com.androidvip.hebf.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_cpu_manager.*
import kotlinx.android.synthetic.main.fragment_cpu_manager_tab.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class CpuManagerActivity : BaseActivity() {
    private lateinit var cpuManager: CpuManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cpu_manager)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(createVectorDrawable(R.drawable.ic_arrow_back))
        }

        val cpuAbi = Utils.getProp("ro.product.cpu.abi", "null")
        val osArch = System.getProperty("os.arch") ?: ""
        if (cpuAbi.contains("x86_64")) {
            Logger.logWarning("This CPU arch ($cpuAbi/$osArch) is currently not supported :(", this)
            MaterialAlertDialogBuilder(this)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage("This CPU arch ($cpuAbi/$osArch) is currently not supported :(")
                    .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
                    .setCancelable(false)
                    .applyAnim().also {
                        if (isAlive) {
                            it.show()
                        }
                    }
        } else {
            lifecycleScope.launch(workerContext) {
                var errorMsg = ""
                try {
                    cpuManager = CpuManager()
                } catch (e: Exception) {
                    errorMsg = "Unable to instantiate the CpuManager: ${e.message}"
                }

                runSafeOnUiThread {
                    if (errorMsg.isNotEmpty()) {
                        showError(errorMsg)
                    } else {
                        setupViewPager()
                    }
                }
            }

            cpuTabLayout.setupWithViewPager(cpuViewPager)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fragment_close_enter, R.anim.fragment_close_exit)
    }

    private fun showError(errorMsg: String?) {
        if (isFinishing) return

        cpuViewPager.adapter?.let { (it as ViewPagerAdapter).clear() }
        errorMsg?.let {
            Logger.logError(it, this)
            cpuErrorText.text = it
            cpuProgress.visibility = View.GONE
            cpuErrorText.visibility = View.VISIBLE
        }
    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(supportFragmentManager)

        lifecycleScope.launch {
            val hardware = cpuManager.getHardware()

            val policies = cpuManager.policies
            if (!policies.isNullOrEmpty() && policies.size in 2..3) {
                policies.forEachIndexed { index, policy ->
                    adapter.addFrag(
                        CpuClusterFragment.newInstance(index),
                        policy.policyName.capitalize(Locale.getDefault())
                    )
                }
            } else {
                adapter.addFrag(CpuClusterFragment.newInstance(0), hardware)
            }

            cpuViewPager.adapter = adapter
        }
    }

    private inner class ViewPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        private val fragmentList = mutableListOf<Fragment>()
        private val fragmentTitleList = mutableListOf<String>()

        override fun getItem(position: Int): Fragment {
            return fragmentList[position]
        }

        override fun getCount(): Int {
            return fragmentList.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return fragmentTitleList[position]
        }

        fun addFrag(fragment: Fragment, title: String) {
            fragmentList.add(fragment)
            fragmentTitleList.add(title)
        }

        fun clear() {
            try {
                val transaction = supportFragmentManager.beginTransaction()
                fragmentList.forEach {
                    transaction.remove(it)
                }
                fragmentList.clear()
                transaction.commitAllowingStateLoss()
            } catch (e: Exception) {}
        }
    }

    class CpuClusterFragment : BaseFragment() {
        private var index = 0
        private var firstFetch = true
        private val handler = Handler()
        private lateinit var getCpuInfoRunnable: Runnable
        private lateinit var cpuManager: CpuManager

        @StringDef("min", "max")
        private annotation class FreqParam

        companion object {
            fun newInstance(index: Int) = CpuClusterFragment().apply {
                arguments = Bundle(1).apply {
                    putInt("index", index)
                }
            }
        }

        private fun String.toMHz() = try {
            "${this.toInt() / 1000} MHz"
        } catch (e: Exception) {
            "800000 MHz"
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            cpuManager = CpuManager()
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_cpu_manager_tab, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            index = arguments?.getInt("index", 0) ?: 0
        }

        override fun onStart() {
            super.onStart()
            setUpHandler()
        }

        override fun onResume() {
            super.onResume()

            handler.removeCallbacks(getCpuInfoRunnable)
            handler.post(getCpuInfoRunnable)
        }

        override fun onPause() {
            super.onPause()
            handler.removeCallbacks(getCpuInfoRunnable)
        }

        private fun setUpHandler() {
            getCpuInfoRunnable = Runnable {
                lifecycleScope.launch {
                    getCpuInfos()
                }
            }
        }

        @SuppressLint("DefaultLocale")
        @Synchronized
        private suspend fun getCpuInfos() = withContext(Dispatchers.Default) {
            val policies = cpuManager.policies
            val cpus = cpuManager.cpus
            val hardware = cpuManager.getHardware()

            if (!policies.isNullOrEmpty() && index >= policies.size) {
                return@withContext
            }

            if (!cpus.isNullOrEmpty() && index >= cpus.size) {
                return@withContext
            }

            val cpuRange = if (!policies.isNullOrEmpty()) {
                policies[index].relatedCPUs.first().rangeTo(policies[index].relatedCPUs.last())
            } else {
                0 until cpus.size
            }

            val governor = if (!policies.isNullOrEmpty()) policies[index].currentGov else cpus[index].currentGov
            val minFreq = if (!policies.isNullOrEmpty()) policies[index].minFreq else cpus[index].minFreq
            val maxFreq = if (!policies.isNullOrEmpty()) policies[index].maxFreq else cpus[index].maxFreq

            val onlineCores = ArrayList<Boolean>()
            val frequencies = ArrayList<Int>()
            val availableGovs = if (!policies.isNullOrEmpty()) policies[index].availableGovs else cpus[index].availableGovs
            val availableFreqs = if (!policies.isNullOrEmpty()) policies[index].availableFreqs else cpus[index].availableFreqs
            val governorTunables = cpus[index].getGovernorTunables()

            for (cpuNum in cpuRange) {
                if (!policies.isNullOrEmpty()) {
                    onlineCores.add(policies[index].isOnline)
                    frequencies.add(try {
                        policies[index].currentFreq.toInt() / 1000
                    } catch (e: Exception) {
                        800000
                    })
                } else {
                    onlineCores.add(cpus[cpuNum].isOnline)
                    frequencies.add(try {
                        cpus[cpuNum].currentFreq.toInt() / 1000
                    } catch (e: Exception) {
                        800000
                    })
                }
            }

            delay(500)
            runSafeOnUiThread {
                cpuManagerGov.text = governor
                cpuManagerMinFreq.text = minFreq.toMHz()
                cpuManagerMaxFreq.text = maxFreq.toMHz()

                if (firstFetch) {
                    firstFetch = false

                    for (i in cpuRange) {
                        cpuManagerCpusContainer.addView(generateCpuSwitch(i, cpuRange))
                    }

                    for ((key, value) in CPUDatabases.data) {
                        if (hardware.toLowerCase().contains(key.toLowerCase())) {
                            view?.let {
                                it.findViewById<View>(R.id.cpuHardwareCard)?.visibility = View.VISIBLE
                                it.findViewById<TextView>(R.id.cpuProcessorName)?.text = key.toUpperCase()
                                it.findViewById<TextView>(R.id.cpuProcessorPlatform)?.text = value

                                val hardwareIcon = it.findViewById<ImageView>(R.id.cpuManagerHardwareIcon)
                                if (hardware.toLowerCase().contains("qualcomm") || hardware.startsWith("sdm") ||
                                        hardware.startsWith("msm")) {
                                    hardwareIcon.setImageResource(R.drawable.ic_hardware_snapdragon)
                                } else if (hardware.toUpperCase().startsWith("MT")) {
                                    hardwareIcon.setImageResource(R.drawable.ic_hardware_mtk)
                                } else if (hardware.toLowerCase().contains("exynos")) {
                                    hardwareIcon.setImageResource(R.drawable.ic_hardware_exynos)
                                } else {
                                    hardwareIcon.visibility = View.GONE
                                }
                            }
                        }
                    }

                    activity?.findViewById<ProgressBar>(R.id.cpuProgress)?.visibility = View.GONE
                    view?.findViewById<ScrollView>(R.id.cpuManagerScroll)?.visibility = View.VISIBLE
                } else if (cpuManagerCpusContainer.childCount >= 1) {
                    for (cpuNum in 0 until cpuManagerCpusContainer.childCount) {
                        val switchCompat = cpuManagerCpusContainer.getChildAt(cpuNum) as SwitchCompat
                        switchCompat.apply {
                            try {
                                setOnCheckedChangeListener(null)
                                isChecked = onlineCores[cpuNum]
                                setOnCheckedChangeListener(coresCheckedChangeListener(tag as Int, cpuRange))
                                text = if (onlineCores[cpuNum])
                                    getString(R.string.cpu_core_status_online, tag as Int, frequencies[cpuNum])
                                else
                                    getString(R.string.cpu_core_status_offline, tag as Int)
                            } catch (ignored: Exception) {
                            }
                        }
                    }
                }

                view?.let { v ->
                    fun setFreq(@FreqParam freqParam: String) {
                        val freqCheck: String
                        val dialogTitle: String

                        if (freqParam == "min") {
                            freqCheck = minFreq
                            dialogTitle = getString(R.string.cpu_min_frequency)
                        } else {
                            freqCheck = maxFreq
                            dialogTitle = getString(R.string.cpu_max_frequency)
                        }

                        var checkedItem = -1
                        for (i in availableFreqs.indices)
                            if (freqCheck == availableFreqs[i])
                                checkedItem = i

                        MaterialAlertDialogBuilder(findContext())
                                .setTitle(dialogTitle)
                                .setSingleChoiceItems(availableFreqs, checkedItem) { dialog, which ->
                                    val selectedFreq = availableFreqs[which]
                                    if (freqParam == "min") {
                                        if (!policies.isNullOrEmpty()) {
                                            policies[index].setMinFreq(selectedFreq)
                                        } else {
                                            cpus[index].setMinFreq(selectedFreq)
                                        }
                                        prefs.putString(K.PREF.CPU_MIN_FREQ, selectedFreq)
                                    } else {
                                        if (!policies.isNullOrEmpty()) {
                                            policies[index].setMaxFreq(selectedFreq)
                                        } else {
                                            cpus[index].setMaxFreq(selectedFreq)
                                        }
                                        prefs.putString(K.PREF.CPU_MAX_FREQ, selectedFreq)
                                    }
                                    dialog.dismiss()
                                }
                                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                                .show()
                    }

                    val layoutTunables = v.findViewById<LinearLayout>(R.id.cpuManagerLayoutGovTunables)
                    try {
                        if (governorTunables.isNotEmpty()) {
                            layoutTunables.getChildAt(0).isEnabled = true
                            layoutTunables.setOnClickListener {
                                Intent(findContext(), CpuTunerActivity::class.java).apply {
                                    putExtra(K.EXTRA_GOVERNOR_TUNABLES, governorTunables)
                                    putExtra("governor", governor)
                                    startActivity(this)
                                    requireActivity().overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out)
                                }
                            }
                        } else {
                            layoutTunables.getChildAt(0).isEnabled = false
                        }
                    } catch (ignored: Exception) {

                    }

                    v.findViewById<LinearLayout>(R.id.cpuManagerLayoutGov).setOnClickListener {
                        var checkedItem = -1
                        for (i in availableGovs.indices) {
                            if (governor == availableGovs[i]) {
                                checkedItem = i
                                break
                            }
                        }

                        MaterialAlertDialogBuilder(findContext())
                                .setTitle(R.string.cpu_governor)
                                .setSingleChoiceItems(availableGovs, checkedItem) { dialog, which ->
                                    val selectedGovernor = availableGovs[which]
                                    if (!policies.isNullOrEmpty()) {
                                        policies[index].setGov(selectedGovernor)
                                    } else {
                                        cpus[index].setGov(selectedGovernor)
                                    }

                                    prefs.putString(K.PREF.CPU_GOV, selectedGovernor)
                                    dialog.dismiss()
                                }
                                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                                .show()
                    }

                    cpuManagerLayoutMinFreq.setOnClickListener { setFreq("min") }
                    cpuManagerLayoutMaxFreq.setOnClickListener { setFreq("max") }
                }
                handler.postDelayed(getCpuInfoRunnable, 500)
            }
        }

        @Synchronized
        private fun generateCpuSwitch(cpuNum: Int, cpuRange: IntRange): SwitchCompat {
            val dp12 = Utils.dpToPx(findContext(), 12)
            return SwitchCompat(findContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
                textSize = 14f
                setOnCheckedChangeListener(coresCheckedChangeListener(cpuNum, cpuRange))
                setPadding(0, dp12, 0, dp12)
                tag = cpuNum
                text = findContext().getString(R.string.cpu_core_status_online).format(cpuNum, 0)
            }
        }

        private fun coresCheckedChangeListener(coreNum: Int, cpuRange: IntRange): CompoundButton.OnCheckedChangeListener {
            return CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                lifecycleScope.launch(workerContext) {
                    var onlineCoresCount = 0
                    for (i in cpuRange)
                        if (cpuManager.cpus[i].isOnline)
                            onlineCoresCount++

                    if (isChecked) {
                        cpuManager.cpus[coreNum].setOnline(true)
                    } else {
                        if (onlineCoresCount > 1) {
                            if (coreNum > cpuRange.first) {
                                cpuManager.cpus[coreNum].setOnline(false)
                            }
                        } else {
                            runSafeOnUiThread {
                                Toast.makeText(findContext(), R.string.unsafe_operation, Toast.LENGTH_SHORT).show()
                                buttonView.isChecked = true
                            }
                        }
                    }
                }
            }
        }
    }
}