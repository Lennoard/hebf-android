package com.androidvip.hebf.ui.main.tune

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
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.R
import com.androidvip.hebf.databinding.ActivityVmBinding
import com.androidvip.hebf.findNearestPositiveMultipleOf
import com.androidvip.hebf.goAway
import com.androidvip.hebf.show
import com.androidvip.hebf.ui.base.binding.BaseViewBindingActivity
import com.androidvip.hebf.utils.*
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class VmActivity : BaseViewBindingActivity<ActivityVmBinding>(ActivityVmBinding::inflate),
    OnParamClickedListener {
    private lateinit var getValuesRunnable: Runnable
    private var shouldPost = false
    private val params = mutableListOf<VmParam>()
    private val handler: Handler by lazy { Handler(mainLooper) }
    private val adapter by lazy { VmParamAdapter(lifecycleScope, this) }
    private val am by lazy {
        getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpToolbar(binding.toolbar)

        shouldPost = true

        binding.onBootSwitch.apply {
            setOnCheckedChangeListener(null)
            isChecked = prefs.getBoolean(K.PREF.VM_ON_BOOT, false)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.VM_ON_BOOT, isChecked)
            }
        }

        with(binding.vmParamsRecyclerView) {
            addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, getRecyclerViewColumns())
        }

        Logger.logDebug("Getting VM params", this)
        lifecycleScope.launch(workerContext) {
            val supportsZram = ZRAM.supported()
            val diskSize = ZRAM.getDiskSize()
            delay(500)
            VM.getParams().forEachIndexed { index, param ->
                params.add(VmParam(index, param, "", getInfoFromParamName(param)))
            }

            System.gc()

            runSafeOnUiThread {
                binding.vmParamsRecyclerView.adapter = adapter
                adapter.updateData(params)

                if (supportsZram) {
                    val currentValue = diskSize.findNearestPositiveMultipleOf(8)
                    binding.zramDiskSizeText.text = "$currentValue MB"
                    binding.zramSeek.apply {
                        runCatching { value = currentValue.toFloat() }
                        addOnChangeListener { _, value, _ ->
                            binding.zramDiskSizeText.text = value.toInt().toString()
                        }
                        addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                            override fun onStartTrackingTouch(slider: Slider) {}

                            override fun onStopTrackingTouch(slider: Slider) {
                                val value = slider.value.toInt()
                                prefs.putInt(K.PREF.VM_DISK_SIZE_MB, slider.value.toInt())
                                Logger.logInfo(
                                    "Setting zram disk size to $value", this@VmActivity
                                )
                                lifecycleScope.launch(workerContext) {
                                    ZRAM.setDiskSize(value.toLong())
                                }
                            }
                        })
                    }
                } else {
                    Logger.logDebug("ZRAM is not supported", this@VmActivity)
                }
            }
        }

        getValuesRunnable = Runnable {
            lifecycleScope.launch(workerContext) {

                val memoryInfo = ActivityManager.MemoryInfo()
                am?.getMemoryInfo(memoryInfo)

                val totalMemory = memoryInfo.totalMem / 1024 / 1024
                val usedMemory = totalMemory - (memoryInfo.availMem / 1024 / 1024)
                val totalSwap = ZRAM.getTotalSwap()
                val usedSwap = totalSwap - ZRAM.getFreeSwap()
                val supportsZRam = ZRAM.supported()

                runSafeOnUiThread {
                    binding.memoryProgress.apply {
                        max = totalMemory.toInt()
                        progress = usedMemory.toInt()
                    }
                    binding.vmMemoryCurrentText.text = "${usedMemory.toInt()} MB"
                    binding.vmMemoryTotal.text = "$totalMemory MB"

                    if (supportsZRam) {
                        binding.swapProgress.apply {
                            max = totalSwap.toInt()
                            progress = usedSwap.toInt()
                        }

                        binding.vmSwapCurrentText.text = "${usedSwap.toInt()} MB"
                        binding.vmSwapTotal.text = "$totalSwap MB"
                    } else {
                        binding.vmZramCard.goAway()
                        binding.swapProgress.progress = 0
                        binding.vmSwapCurrentText.text = "N/A"
                    }

                    binding.vmProgress.goAway()
                    binding.vmScroll.show()

                    if (shouldPost) {
                        handler.postDelayed(getValuesRunnable, 4000)
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
        overridePendingTransition(R.anim.fragment_close_enter, R.anim.slide_out_right)
    }

    override fun onParamClicked(param: VmParam) {
        EditDialog(this).buildApplying {
            title = param.displayName
            inputText = param.value
            inputHint = param.value
            inputType = InputType.TYPE_CLASS_TEXT
            guessInputType = true
            onConfirmListener = object : EditDialog.OnConfirmListener {
                override fun onOkButtonClicked(newData: String) {
                    if (newData.isEmpty() || !param.isValidParam()) return
                    params[param.index] = param
                    adapter.updateData(params)

                    prefs.putString("vm_${param.name}", newData)
                    Logger.logInfo(
                        "Setting vm param ${param.name} to $newData",
                        this@VmActivity
                    )
                    lifecycleScope.launch(workerContext) {
                        VM.setValue(param.name, newData)
                    }
                }
            }
        }.show()
    }

    override fun onInfoClicked(param: VmParam) {
        ModalBottomSheet.newInstance(param.displayName, param.info)
            .show(supportFragmentManager, "sheet")
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
        private val scope: LifecycleCoroutineScope,
        private val onParamClickedListener: OnParamClickedListener
    ) : RecyclerView.Adapter<VmParamAdapter.ViewHolder>() {
        private val paramList: MutableList<VmParam> = mutableListOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_generic_param, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val param = paramList[position]

            holder.name.text = param.displayName

            scope.launch(Dispatchers.Default) {
                val paramValue = VM.getValue(param.name)
                param.value = paramValue
                withContext(Dispatchers.Main) {
                    holder.value.text = paramValue
                }
            }

            if (!param.info.isNullOrEmpty()) {
                holder.info.show()
                holder.info.setOnClickListener {
                    onParamClickedListener.onInfoClicked(param)
                }
            } else {
                holder.info.goAway()
            }

            holder.itemView.setOnClickListener {
                onParamClickedListener.onParamClicked(param)
            }
        }

        override fun getItemCount(): Int {
            return paramList.size
        }

        fun updateData(list: MutableList<VmParam>) {
            paramList.clear()
            paramList.addAll(list)
            notifyDataSetChanged()
        }

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            var name: TextView = v.findViewById(R.id.paramName)
            var value: TextView = v.findViewById(R.id.paramValue)
            val info: ImageView = v.findViewById(R.id.paramInfo)
        }
    }

    data class VmParam(
        val index: Int,
        val name: String,
        var value: String,
        val info: String? = null
    ) {
        val displayName: String
            get() = name.capitalize(Locale.getDefault())
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
}

interface OnParamClickedListener {
    fun onParamClicked(param: VmActivity.VmParam)
    fun onInfoClicked(param: VmActivity.VmParam)
}
