package com.androidvip.hebf.activities

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.R
import com.androidvip.hebf.findNearestPositiveMultipleOf
import com.androidvip.hebf.goAway
import com.androidvip.hebf.show
import com.androidvip.hebf.utils.*
import kotlinx.android.synthetic.main.activity_lmk.toolbar
import kotlinx.android.synthetic.main.activity_vm.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class VmActivity : BaseActivity() {
    private val handler: Handler by lazy { Handler() }
    private lateinit var getValuesRunnable: Runnable
    private var shouldPost = false

    data class VmParam(val name: String, var value: String, val info: String? = null) {
        val displayName: String
            get() = name.capitalize()
                .replace("_", " ")
                .replace("Oom", "OOM")

        fun isValidParam(): Boolean {
            return !name.contains("/")
                && !name.contains("/")
                && !name.contains(".")
                && !name.contains(",")
                && !name.contains("#")
                && !name.contains("$")
                && !name.contains("[")
                && !name.contains("]")
                && !name.contains(" ")
        }

    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vm)
        setUpToolbar(toolbar)

        val prefs = Prefs(applicationContext)

        shouldPost = true

        vmOnBootSwitch.apply {
            setOnCheckedChangeListener(null)
            isChecked = prefs.getBoolean(K.PREF.VM_ON_BOOT, false)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.VM_ON_BOOT, isChecked)
            }
        }


        with(vmParamsRecyclerView) {
            addItemDecoration(DividerItemDecoration(vmParamsRecyclerView.context, LinearLayout.VERTICAL))
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this.context, getRecyclerViewColumns())
        }

        Logger.logDebug("Getting VM params", this)
        lifecycleScope.launch(workerContext) {
            val supportsZram = ZRAM.supported()
            val diskSize = ZRAM.getDiskSize()
            val vmParams = mutableListOf<VmParam>()

            VM.getParams().forEach { param ->
                vmParams.add(VmParam(param, "", getInfoFromParamName(param)))
            }

            System.gc()

            runSafeOnUiThread {
                vmParamsRecyclerView.adapter = VmParamAdapter(vmParams, this@VmActivity)

                if (supportsZram) {
                    vmZramDiskSizeText.text = "${diskSize.findNearestPositiveMultipleOf(8)} MB"
                    vmZramSeek.progress = diskSize.findNearestPositiveMultipleOf(8)
                    vmZramSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            var newProgress = progress
                            val step = 8
                            newProgress = (newProgress / step).toFloat().roundToInt() * step
                            seekBar?.progress = newProgress

                            vmZramDiskSizeText.text = newProgress.toString()
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            seekBar?.let {
                                prefs.putInt(K.PREF.VM_DISK_SIZE_MB, it.progress)
                                Logger.logInfo("Setting zram disk size to ${it.progress}", this@VmActivity)
                                launch(workerContext) {
                                    ZRAM.setDiskSize(it.progress.toLong())
                                }
                            }
                        }
                    })
                } else {
                    Logger.logDebug("ZRAM is not supported", this@VmActivity)
                }
            }
        }

        getValuesRunnable = Runnable {
            lifecycleScope.launch(workerContext) {
                val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
                val memoryInfo = ActivityManager.MemoryInfo()
                am?.getMemoryInfo(memoryInfo)

                val totalMemory = memoryInfo.totalMem / 1024 / 1024
                val usedMemory = totalMemory - (memoryInfo.availMem / 1024 / 1024)
                val totalSwap = ZRAM.getTotalSwap()
                val usedSwap = totalSwap - ZRAM.getFreeSwap()
                val supportsZRam = ZRAM.supported()

                runSafeOnUiThread {
                    vmMemoryProgress.max = totalMemory.toInt()
                    vmMemoryProgress.progress = usedMemory.toInt()
                    vmMemoryCurrentText.text = "${usedMemory.toInt()} MB"
                    vmMemoryTotal.text = "$totalMemory MB"

                    if (supportsZRam) {
                        vmSwapProgress.max = totalSwap.toInt()
                        vmSwapProgress.progress = usedSwap.toInt()
                        vmSwapCurrentText.text = "${usedSwap.toInt()} MB"
                        vmSwapTotal.text = "$totalSwap MB"
                    } else {
                        vmZramCard.goAway()
                        vmSwapProgress.progress = 0
                        vmSwapCurrentText.text = "N/A"
                    }

                    vmProgress.goAway()
                    vmScroll.show()

                    if (shouldPost) {
                        handler.postDelayed(getValuesRunnable, 2000)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        shouldPost = true
        handler.post(getValuesRunnable)
    }

    override fun onStop() {
        super.onStop()

        shouldPost = false
        handler.removeCallbacks(getValuesRunnable)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fragment_close_enter, android.R.anim.slide_out_right)
    }

    private fun getInfoFromParamName(param: String): String {
        return when (param) {
            "admin_reserve_kbytes" -> getString(R.string.vm_admin_reserve_kbytes)
            "block_dump" -> getString(R.string.vm_block_dump)
            "compact_memory" -> getString(R.string.vm_compact_memory)
            "compact_unevictable_allowed" -> getString(R.string.vm_compact_unevictable_allowed)
            "dirty_background_bytes" -> getString(R.string.vm_dirty_background_bytes)
            "dirty_background_ratio" -> getString(R.string.vm_dirty_background_ratio)
            "dirty_bytes" -> getString(R.string.vm_dirty_bytes)
            "dirty_expire_centisecs" -> getString(R.string.vm_dirty_expire_centisecs)
            "dirty_ratio" -> getString(R.string.vm_dirty_ratio)
            "dirtytime_expire_seconds" -> getString(R.string.vm_dirtytime_expire_seconds)
            "dirty_writeback_centisecs" -> getString(R.string.vm_dirty_writeback_centisecs)
            "extfrag_threshold" -> getString(R.string.vm_extfrag_threshold)
            "highmem_is_dirtyable" -> getString(R.string.vm_highmem_is_dirtyable)
            "laptop_mode" -> getString(R.string.vm_laptop_mode)
            "legacy_va_layout" -> getString(R.string.vm_legacy_va_layout)
            "max_map_count" -> getString(R.string.vm_max_map_count)
            "min_free_kbytes" -> getString(R.string.vm_min_free_kbytes)
            "mmap_min_addr" -> getString(R.string.vm_mmap_min_addr)
            "mmap_rnd_bits" -> getString(R.string.vm_mmap_rnd_bits)
            "mmap_rnd_compat_bits" -> getString(R.string.vm_mmap_rnd_compat_bits)
            "oom_dump_tasks" -> getString(R.string.vm_oom_dump_tasks)
            "oom_kill_allocating_task" -> getString(R.string.vm_oom_kill_allocating_task)
            "overcommit_kbytes" -> getString(R.string.vm_overcommit_kbytes)
            "overcommit_memory" -> getString(R.string.vm_overcommit_memory)
            "overcommit_ratio" -> getString(R.string.vm_overcommit_ratio)
            "page-cluster" -> getString(R.string.vm_page_cluster)
            "percpu_pagelist_fraction" -> getString(R.string.vm_percpu_pagelist_fraction)
            "stat_interval" -> getString(R.string.vm_stat_interval)
            "swappiness" -> getString(R.string.vm_swappiness)
            else -> ""
        }
    }

    private fun getRecyclerViewColumns(): Int {
        val isLandscape = resources.getBoolean(R.bool.is_landscape)
        return if (isLandscape) 2 else 1
    }

    class VmParamAdapter(
        private val paramList: MutableList<VmParam>,
        private val activity: BaseActivity
    ) : RecyclerView.Adapter<VmParamAdapter.ViewHolder>() {
        val prefs: Prefs by lazy { Prefs(activity.applicationContext) }

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            var name: TextView = v.findViewById(R.id.paramName)
            var value: TextView = v.findViewById(R.id.paramValue)
            val info: ImageView = v.findViewById(R.id.paramInfo)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.list_item_generic_param, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val param = paramList[position]

            holder.name.text = param.displayName

            activity.lifecycleScope.launch(activity.workerContext) {
                val paramValue = VM.getValue(param.name)
                activity.runSafeOnUiThread {
                    holder.value.text = paramValue
                }
            }

            if (!param.info.isNullOrEmpty()) {
                holder.info.show()
                holder.info.setOnClickListener {
                    ModalBottomSheet.newInstance(param.displayName, param.info).show((activity as AppCompatActivity).supportFragmentManager, "sheet")
                }
            } else {
                holder.info.goAway()
            }

            holder.itemView.setOnClickListener {
                EditDialog(activity).buildApplying {
                    title = param.displayName
                    inputText = param.value
                    inputHint = param.value
                    inputType = InputType.TYPE_CLASS_TEXT
                    guessInputType = true
                    onConfirmListener = object : EditDialog.OnConfirmListener {
                        override fun onOkButtonClicked(newData: String) {
                            if (newData.isNotEmpty()) {
                                holder.value.text = newData
                                if (param.isValidParam()) {
                                    prefs.putString("vm_${param.name}", newData)
                                    Logger.logInfo("Setting vm param ${param.name} to $newData", activity)
                                    VM.setValue(param.name, newData)
                                }
                            }
                        }
                    }
                }.show()
            }
        }

        override fun getItemCount(): Int {
            return paramList.size
        }
    }

}
