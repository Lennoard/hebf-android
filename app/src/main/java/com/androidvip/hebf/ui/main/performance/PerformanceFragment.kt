package com.androidvip.hebf.ui.main.performance

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.R
import com.androidvip.hebf.databinding.FragmentPerformance2Binding
import com.androidvip.hebf.runSafeOnUiThread
import com.androidvip.hebf.services.gb.GameJobService
import com.androidvip.hebf.show
import com.androidvip.hebf.ui.base.binding.BaseViewBindingFragment
import com.androidvip.hebf.ui.main.LottieAnimViewModel
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.Logger
import com.androidvip.hebf.utils.Logger.logError
import com.androidvip.hebf.utils.RootUtils
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class PerformanceFragment : BaseViewBindingFragment<FragmentPerformance2Binding>(
    FragmentPerformance2Binding::inflate
) {
    private val animViewModel: LottieAnimViewModel by sharedViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch(workerContext) {
            val isRooted = isRooted()
            runSafeOnUiThread {
                setControls(isRooted)
            }
        }

        binding.gameBoosterCard.setOnClickListener {
            GameBoosterSheetFragment().show(childFragmentManager, "gameSheet")
        }
    }

    override fun onResume() {
        super.onResume()
        animViewModel.setAnimRes(R.raw.performance_anim)
    }

    private fun setControls(isRooted: Boolean) {
        binding.gpu.apply {
            isEnabled = isRooted
            setOnCheckedChangeListener(null)
            setChecked(prefs.getBoolean(K.PREF.PERFORMANCE_GPU, false))
            setOnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.PERFORMANCE_GPU, isChecked)
                if (isChecked) {
                    runCommands(
                        "setprop hwui.render_dirty_regions false",
                        "setprop persist.sys.ui.hw 1",
                        "setprop debug.egl.hw 1",
                        "setprop debug.composition.type gpu"
                    )
                    Logger.logInfo("GPU Tweaks enabled", applicationContext)
                } else {
                    Logger.logInfo("Removed GPU tweaks", applicationContext)
                    runCommands(
                        "mount -o remount,rw /data",
                        "rm -f /data/property/persist.sys.ui.hw",
                        "mount -o remount,ro /data"
                    )
                }
            }
        }

        binding.render.apply {
            isEnabled = isRooted
            setOnCheckedChangeListener(null)
            setChecked(prefs.getBoolean(K.PREF.PERFORMANCE_RENDERING, false))
            setOnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.PERFORMANCE_RENDERING, isChecked)
                if (isChecked) {
                    RootUtils.runInternalScriptAsync("ren_on", findContext())
                    Logger.logInfo("Rendering tweaks enabled", applicationContext)
                } else {
                    Logger.logInfo("Removed rendering tweaks", applicationContext)
                }
            }
        }

        binding.ring.apply {
            isEnabled = isRooted
            setOnCheckedChangeListener(null)
            setChecked(prefs.getBoolean(K.PREF.PERFORMANCE_CALL_RING, false))
            setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.PERFORMANCE_CALL_RING, isChecked)
                if (isChecked) {
                    runCommand("setprop ro.telephony.call_ring.delay 0")
                } else {
                    runCommand("setprop ro.telephony.call_ring.delay 1")
                }
            })
        }

        binding.lsUi.apply {
            isEnabled = isRooted
            setOnCheckedChangeListener(null)
            setChecked(prefs.getBoolean(K.PREF.PERFORMANCE_LS_UI, false))
            setOnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.PERFORMANCE_LS_UI, isChecked)
                if (isChecked) {
                    Logger.logInfo("LSUI: Animation scale set to 0.7", applicationContext)
                    runCommands(
                        "settings put global transition_animation_scale 0.7",
                        "settings put global animator_duration_scale 0.7",
                        "settings put global window_animation_scale 0.7",
                        "setprop persist.service.lgospd.enable 0",
                        "setprop persist.service.pcsync.enable 0",
                        "setprop touch.pressure.scale 0.001"
                    )
                } else {
                    runCommands(
                        "settings put global transition_animation_scale 1.0",
                        "settings put global animator_duration_scale 1.0",
                        "settings put global window_animation_scale 1.0",
                        "mount -o remount,rw /data",
                        "rm -f /data/property/persist.service.pcsync.enable",
                        "rm -f /data/property/persist.service.lgospd.enable",
                        "mount -o remount,ro /data"
                    )

                    Logger.logInfo("LSUI: Animation scale set to 1.0", applicationContext)
                }
            }
        }

        binding.scroll.apply {
            isEnabled = isRooted
            setOnCheckedChangeListener(null)
            setChecked(prefs.getBoolean(K.PREF.PERFORMANCE_SCROLLING, false))
            setOnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.PERFORMANCE_SCROLLING, isChecked)
                if (isChecked) {
                    Logger.logInfo("Added scrolling tweaks", applicationContext)
                    RootUtils.runInternalScriptAsync("ro_on", findContext())
                } else {
                    Logger.logInfo("Removed scrolling tweaks", applicationContext)
                }
            }
        }

        binding.fps.apply {
            isEnabled = isRooted
            setOnCheckedChangeListener(null)
            setChecked(prefs.getBoolean(K.PREF.PERFORMANCE_FPS_UNLOCKER, false))
            setOnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.PERFORMANCE_FPS_UNLOCKER, isChecked)
                if (isChecked) {
                    runCommand("setprop debug.egl.swapinterval -60")
                } else {
                    runCommand("setprop debug.egl.swapinterval 1")
                }
            }
        }

        binding.performance.apply {
            setOnCheckedChangeListener(null)
            setChecked(prefs.getBoolean(K.PREF.PERFORMANCE_PERF_TWEAK, false))
            setOnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.PERFORMANCE_PERF_TWEAK, isChecked)
                if (isChecked) {
                    if (isRooted) {
                        RootUtils.runInternalScriptAsync("pf_on", findContext())
                        runCommands(
                            "setprop persist.sys.use_dithering 0",
                            "setprop persist.sys.use_16bpp_alpha 1"
                        )
                        Logger.logInfo("Added performance tweaks", applicationContext)
                    } else {
                        prefs.putBoolean(K.PREF.LESS_AUTO_OPT_PER, true)
                        toggleGameService(true, findContext())
                    }
                } else {
                    if (isRooted) {
                        runCommands(
                            "mount -o remount,rw /data",
                            "rm -f /data/property/persist.sys.use_dithering",
                            "rm -f /data/property/setprop persist.sys.use_16bpp_alpha",
                            "mount -o remount,ro /data"
                        )
                        Logger.logInfo("Removed performance tweaks", applicationContext)
                    } else {
                        prefs.putBoolean(K.PREF.LESS_AUTO_OPT_PER, false)
                        toggleGameService(false, findContext())
                    }
                }
            }
        }

        if (!isRooted) {
            binding.optimize.apply {
                show()
                setOnClickListener {
                    throw NotImplementedError()
                }
            }
        }
    }

    private fun toggleGameService(start: Boolean, context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val millis = (15 * 60 * 1000).toLong()
            val componentName = ComponentName(context, GameJobService::class.java)
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
            val builder = JobInfo.Builder(K.GAME_LESS_JOB_ID, componentName).apply {
                setMinimumLatency(millis)
                setOverrideDeadline(millis + 5 * 60 * 1000)
                setRequiresCharging(false)
            }
            if (jobScheduler != null) {
                if (start) {
                    jobScheduler.schedule(builder.build())
                    prefs.putBoolean(K.PREF.GB_IS_SCHEDULED_LESS, true)
                } else {
                    jobScheduler.cancel(K.GAME_LESS_JOB_ID)
                    prefs.putBoolean(K.PREF.GB_IS_SCHEDULED_LESS, false)
                }
            } else {
                logError("Could not schedule job game service", context)
            }
        }
    }
}