package com.androidvip.hebf.activities.advanced

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.R
import com.androidvip.hebf.activities.BaseActivity
import com.androidvip.hebf.goAway
import com.androidvip.hebf.helpers.HebfApp
import com.androidvip.hebf.show
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.Logger.logError
import com.androidvip.hebf.utils.Logger.logInfo
import com.androidvip.hebf.utils.Logger.logWarning
import com.androidvip.hebf.utils.RootUtils
import com.androidvip.hebf.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_art_compiler_filter.*
import kotlinx.coroutines.launch
import java.util.*

// FIXME: Legacy
class ArtCompilerFilter : BaseActivity() {
    private var isSummaryExpanded = true
    private var isFilterExpanded = false
    private var errorString = ""
    private var vmSelection = 0
    private var vmImageSelection = 1
    private val setUpCommands: MutableList<String> = ArrayList()
    private var shouldShowDialog = false
    private var noValueSet = false
    private var propVmFilter = ""
    private var propVmImageFilter = ""
    private var buildPropVmFilter = ""
    private var buildPropVmImageFilter = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_art_compiler_filter)

        setUpToolbar(toolbar)

        val filters = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            arrayOf("quicken", "speed", "verify")
        } else {
            arrayOf("interpret-only", "speed", "balanced", "space", "everything", "verify-none")
        }

        artScroll.setOnScrollChangeListener { _: NestedScrollView?, _: Int, scrollY: Int, _: Int, oldScrollY: Int ->
            if (scrollY > oldScrollY) fab.hide() else fab.show()
        }

        lifecycleScope.launch(workerContext) {
            RootUtils.copyFile("/system/build.prop", "${K.HEBF.HEBF_FOLDER}/build.prop")
            logInfo(
                "Backing up build.prop to ${K.HEBF.HEBF_FOLDER}. Reason: ART Compiler Filter",
                this@ArtCompilerFilter
            )

            propVmFilter = Utils.getProp("dalvik.vm.dex2oat-filter", "error")
            propVmImageFilter = Utils.getProp(
                "dalvik.vm.image-dex2oat-filter", "error"
            )
            buildPropVmFilter = RootUtils.executeWithOutput(
                "grep -F dalvik.vm.dex2oat-filter /system/build.prop", "error"
            )
            buildPropVmImageFilter = RootUtils.executeWithOutput(
                "grep -F dalvik.vm.image-dex2oat-filter /system/build.prop",
                "error"
            )

            setUpCommands.add("mount -o rw,remount /system")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setUpCommands.add("mount -o rw,remount /")
            }

            if (buildPropVmImageFilter.isEmpty()) {
                logWarning("Additional interpreter filter value not found", this@ArtCompilerFilter)
                // Speed is still a valid interpreter filter for Oreo
                setUpCommands.add("setprop dalvik.vm.image-dex2oat-filter speed")
                setUpCommands.add("echo \"dalvik.vm.image-dex2oat-filter=speed\" >> /system/build.prop")
                noValueSet = true
            }
            if (buildPropVmFilter.isEmpty()) {
                setUpCommands.add("setprop dalvik.vm.dex2oat-filter speed")
                setUpCommands.add("echo \"dalvik.vm.dex2oat-filter=speed\" >> /system/build.prop")
                noValueSet = true
            }

            setUpCommands.add("mount -o ro,remount /system")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setUpCommands.add("mount -o ro,remount /")
            }

            if (propVmFilter.isNotEmpty()) {
                var validVmFilterSet = false
                filters.forEach {
                    if (propVmFilter == it) validVmFilterSet = true
                }
                if (!validVmFilterSet) {
                    errorString = "${errorString}No valid value for the current interpreter filter found.\n"
                    logError("No valid value for the current interpreter filter found", this@ArtCompilerFilter)
                    shouldShowDialog = true
                } else {
                    // Pre-Oreo filters
                    if (propVmFilter == "interpret-only") vmSelection = 0
                    if (propVmFilter == "speed") // Both Oreo and Pre-Oreo use this filter, which is at the position 1 in both filters array
                        vmSelection = 1
                    if (propVmFilter == "balanced") vmSelection = 2
                    if (propVmFilter == "space") vmSelection = 3
                    if (propVmFilter == "everything") vmSelection = 4
                    if (propVmFilter == "verify-none") vmSelection = 5

                    // Oreo filters
                    if (propVmFilter == "quicken") vmSelection = 0
                    if (propVmFilter == "verify") vmSelection = 2
                    val sanityTest = "dalvik.vm.dex2oat-filter=$propVmFilter"
                    if (buildPropVmFilter != "error") {
                        if (buildPropVmFilter != sanityTest) {
                            shouldShowDialog = true
                            logError("Current active value does not match the set one: $buildPropVmFilter | $sanityTest", this@ArtCompilerFilter)
                            errorString = "${errorString}Current active value does not match the set one. Didn't you reboot?\n"
                        }
                    }
                }
            }
            if (propVmImageFilter.isNotEmpty()) {
                if (propVmImageFilter != "everything" && propVmImageFilter != "speed" && propVmImageFilter != "balanced"
                    && propVmImageFilter != "space" && propVmImageFilter != "interpret-only" && propVmImageFilter != "verify-none") {
                    errorString = "${errorString}No valid value for the current additional interpreter filter found.\n"
                    logError("No valid value for the current additional interpreter filter found", this@ArtCompilerFilter)
                    shouldShowDialog = true
                } else {
                    // Pre-Oreo filters
                    vmImageSelection = when (propVmFilter) {
                        "interpret-only", "quicken" -> 0
                        "speed" -> 1
                        "balanced", "verify" -> 2
                        "space" -> 3
                        "everything" -> 4
                        "verify-none" -> 5

                        else -> 0
                    }

                    val sanityTestImage = "dalvik.vm.image-dex2oat-filter=$propVmImageFilter"
                    if (buildPropVmImageFilter != "error") {
                        if (buildPropVmImageFilter != sanityTestImage) {
                            shouldShowDialog = true
                            errorString = "${errorString}Current additional active value does not match the set one. Didn't you reboot?\n"
                        }
                    }
                }
            }
            runSafeOnUiThread {
                if (noValueSet) {
                    try {
                        MaterialAlertDialogBuilder(this@ArtCompilerFilter)
                            .setTitle(R.string.error)
                            .setCancelable(false)
                            .setMessage(R.string.art_warning_not_found)
                            .setPositiveButton(getString(R.string.enable)) { _, _ ->
                                RootUtils.execute(setUpCommands, this@ArtCompilerFilter, Runnable {
                                    artVmSpinner.setSelection(vmSelection)
                                    artVmImageSpinner.setSelection(vmImageSelection)
                                    artProgress.goAway()
                                    fab.show()
                                    artProgress.goAway()
                                    artScroll.show()
                                })
                            }
                            .setNegativeButton(getString(R.string.close)) { _: DialogInterface?, _: Int -> onBackPressed() }.show()
                    } catch (e: Exception) {
                        Toast.makeText(this@ArtCompilerFilter, "Something went wrong  :/", Toast.LENGTH_LONG).show()
                        logError(e, applicationContext)
                    }
                } else {
                    if (shouldShowDialog) {
                        try {
                            MaterialAlertDialogBuilder(applicationContext)
                                .setTitle(R.string.error)
                                .setCancelable(false)
                                .setMessage("These errors were found and for safety, we have blocked this option\n\n$errorString")
                                .setPositiveButton("IGNORE") { _, _ ->
                                    artVmSpinner.setSelection(vmSelection)
                                    artVmImageSpinner.setSelection(vmImageSelection)
                                    artProgress.visibility = View.GONE
                                    fab.show()
                                    artProgress.visibility = View.GONE
                                    artScroll.visibility = View.VISIBLE
                                }
                                .setNegativeButton(getString(R.string.close)) { _, _ ->
                                    onBackPressed()
                                }.show()
                        } catch (e: Exception) {
                            logError(e, applicationContext)
                        }
                    } else {
                        artVmSpinner.setSelection(vmSelection)
                        artVmImageSpinner.setSelection(vmImageSelection)
                        artProgress.visibility = View.GONE
                        fab.show()
                        artProgress.visibility = View.GONE
                        artScroll.visibility = View.VISIBLE
                        runCatching {
                            findViewById<View>(R.id.art_root_layout_land).show()
                        }
                    }
                }
            }
        }

        artInfo.setOnClickListener {
            Utils.webDialog(this, "https://source.android.com/devices/tech/dalvik/configure")
        }

        val adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, filters
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        artVmSpinner.adapter = adapter
        artVmSpinner.onItemSelectedListener = null
        artVmImageSpinner.adapter = adapter
        artVmImageSpinner.onItemSelectedListener = null
        fab.setOnClickListener {
            MaterialAlertDialogBuilder(this@ArtCompilerFilter)
                .setTitle(getString(R.string.warning))
                .setMessage("${getString(R.string.confirmation_message)}\nThis may cause bootloop in some devices. A build.prop backup can be found in /internalStorage/HEBF, just in case.")
                .setNegativeButton(R.string.cancelar) { _, _ -> }
                .setPositiveButton("OK") { _: DialogInterface?, _: Int ->
                    lifecycleScope.launch(workerContext) {
                        val selectedFilter = artVmSpinner.selectedItem.toString()
                        val selectedImageFilter = artVmImageSpinner.selectedItem.toString()
                        logInfo("Setting compiler filter $propVmFilter", applicationContext)
                        RootUtils.execute(arrayOf(
                            "mount -o rw,remount /system",
                            "mount -o rw,remount /",
                            "chmod 644 /system/build.prop",
                            "sed 's|dalvik.vm.dex2oat-filter=$propVmFilter|dalvik.vm.dex2oat-filter=$selectedFilter|g' -i /system/build.prop",
                            "sed 's|dalvik.vm.image-dex2oat-filter=$propVmImageFilter|dalvik.vm.image-dex2oat-filter=$selectedImageFilter|g' -i /system/build.prop",
                            "mount -o ro,remount /system",
                            "mount -o ro,remount /"
                        ))
                        runSafeOnUiThread {
                            MaterialAlertDialogBuilder(this@ArtCompilerFilter)
                                .setTitle(getString(R.string.done))
                                .setMessage("${getString(R.string.art_cache_warning)}\n${getString(R.string.wipe_dalvik_now)}")
                                .setNegativeButton(R.string.cancelar) { _: DialogInterface?, _: Int -> }
                                .setPositiveButton("OK") { _: DialogInterface?, _: Int ->
                                    lifecycleScope.launch(workerContext) {
                                        RootUtils.execute("rm -rf /data/dalvik-cache && reboot")
                                    }
                                }
                                .show()
                        }
                    }
                }
                .show()
        }
        var prompt: String? = ""
        runCatching {
            val inputStream = resources.openRawResource(R.raw.compiler_filters)
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            prompt = String(buffer)
            inputStream.close()
        }
        artWebView.loadData(prompt, "text/html", "utf-8")

        artShowMoreSummary.setOnClickListener {
            isSummaryExpanded = if (!isSummaryExpanded) {
                artSummaryText.visibility = View.VISIBLE
                artInfo.visibility = View.VISIBLE
                artShowMoreSummary.setImageResource(R.drawable.ic_up)
                true
            } else {
                artSummaryText.visibility = View.GONE
                artInfo.visibility = View.GONE
                artShowMoreSummary.setImageResource(R.drawable.ic_down)
                false
            }
        }
        artShowMoreFilters.setOnClickListener {
            isFilterExpanded = if (!isFilterExpanded) {
                artWebView.visibility = View.VISIBLE
                artShowMoreFilters.setImageResource(R.drawable.ic_up)
                true
            } else {
                artWebView.visibility = View.GONE
                artShowMoreFilters.setImageResource(R.drawable.ic_down)
                false
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fragment_close_enter, android.R.anim.slide_out_right)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        finish()
    }

    public override fun onDestroy() {
        errorString = ""
        super.onDestroy()
    }
}