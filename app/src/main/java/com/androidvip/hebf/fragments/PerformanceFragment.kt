package com.androidvip.hebf.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import com.androidvip.hebf.R
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.Logger
import com.androidvip.hebf.utils.RootUtils
import com.androidvip.hebf.views.ControlSwitch
import com.google.android.material.snackbar.Snackbar

class PerformanceFragment : BaseFragment() {
    private lateinit var performanceTweak: ControlSwitch
    private lateinit var multitasking: ControlSwitch
    private lateinit var lsUi: ControlSwitch
    private lateinit var rolar: ControlSwitch
    private lateinit var callRingDelay: ControlSwitch
    private lateinit var gpuTweaks: ControlSwitch
    private lateinit var renderingTweaks: ControlSwitch
    private lateinit var fps: ControlSwitch

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_performance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)

        gpuTweaks.setOnCheckedChangeListener(null)
        gpuTweaks.setChecked(prefs.getBoolean(K.PREF.PERFORMANCE_GPU, false))
        gpuTweaks.setOnCheckedChangeListener { _, isChecked ->
            prefs.putBoolean(K.PREF.PERFORMANCE_GPU, isChecked)
            if (isChecked) {
                runCommand(arrayOf(
                    "setprop hwui.render_dirty_regions false",
                    "setprop persist.sys.ui.hw 1",
                    "setprop debug.egl.hw 1",
                    "setprop debug.composition.type gpu"))
                Snackbar.make(gpuTweaks, R.string.gpu_on, Snackbar.LENGTH_SHORT).show()
                logInfo("GPU Tweaks enabled", findContext())
            } else {
                Snackbar.make(gpuTweaks, R.string.gpu_off, Snackbar.LENGTH_SHORT).show()
                logInfo("Removed GPU tweaks", findContext())
                runCommand(arrayOf(
                    "mount -o remount,rw /data",
                    "rm -f /data/property/persist.sys.ui.hw",
                    "mount -o remount,ro /data"))
            }
        }

        renderingTweaks.setOnCheckedChangeListener(null)
        renderingTweaks.setChecked(prefs.getBoolean(K.PREF.PERFORMANCE_RENDERING, false))
        renderingTweaks.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener{ _, isChecked ->
            prefs.putBoolean(K.PREF.PERFORMANCE_RENDERING, isChecked)
            if (isChecked) {
                RootUtils.runInternalScriptAsync("ren_on", findContext())
                logInfo("Rendering tweaks enabled", context)
                Snackbar.make(renderingTweaks, R.string.render_on, Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(renderingTweaks, R.string.render_off, Snackbar.LENGTH_SHORT).show()
                logInfo("Removed rendering tweaks", context)
            }
        })

        callRingDelay.setOnCheckedChangeListener(null)
        callRingDelay.setChecked(prefs.getBoolean(K.PREF.PERFORMANCE_CALL_RING, false))
        callRingDelay.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener{ _, isChecked ->
            prefs.putBoolean(K.PREF.PERFORMANCE_CALL_RING, isChecked)
            if (isChecked) {
                runCommand("setprop ro.telephony.call_ring.delay 0")
                Snackbar.make(callRingDelay, R.string.ring_on, Snackbar.LENGTH_SHORT).show()
            } else {
                runCommand("setprop ro.telephony.call_ring.delay 1")
                Snackbar.make(callRingDelay, R.string.ring_off, Snackbar.LENGTH_SHORT).show()
            }
        })

        performanceTweak.setOnCheckedChangeListener(null)
        performanceTweak.setChecked(prefs.getBoolean(K.PREF.PERFORMANCE_PERF_TWEAK, false))
        performanceTweak.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener{ _, isChecked ->
            prefs.putBoolean(K.PREF.PERFORMANCE_PERF_TWEAK, isChecked)
            if (isChecked) {
                RootUtils.runInternalScriptAsync("pf_on", findContext())
                runCommand(arrayOf(
                        "setprop persist.sys.use_dithering 0",
                        "setprop persist.sys.use_16bpp_alpha 1"))
                Snackbar.make(performanceTweak, R.string.pf_on, Snackbar.LENGTH_SHORT).show()
                logInfo("Added performance tweaks", findContext())
            } else {
                runCommand(arrayOf(
                        "mount -o remount,rw /data",
                        "rm -f /data/property/persist.sys.use_dithering",
                        "rm -f /data/property/setprop persist.sys.use_16bpp_alpha",
                        "mount -o remount,ro /data"))
                logInfo("Removed performance tweaks", findContext())
                Snackbar.make(performanceTweak, R.string.pf_off, Snackbar.LENGTH_SHORT).show()
            }
        })

        multitasking.setOnCheckedChangeListener(null)
        multitasking.setChecked(prefs.getBoolean(K.PREF.PERFORMANCE_MULTITASKING, false))
        multitasking.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener{ _, isChecked ->
            prefs.putBoolean(K.PREF.PERFORMANCE_MULTITASKING, isChecked)
            if (isChecked) {
                RootUtils.runInternalScriptAsync("yrolram", findContext())
                logInfo("Multitasking profile set", context)
                Snackbar.make(multitasking, R.string.multitasking_on, Snackbar.LENGTH_SHORT).show()
            } else {
                logInfo("Multitasking profile unset", context)
                Snackbar.make(multitasking, R.string.multitasking_off, Snackbar.LENGTH_SHORT).show()
            }
        })

        lsUi.setOnCheckedChangeListener(null)
        lsUi.setChecked(prefs.getBoolean(K.PREF.PERFORMANCE_LS_UI, false))
        lsUi.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener{ _, isChecked ->
            prefs.putBoolean(K.PREF.PERFORMANCE_LS_UI, isChecked)
            if (isChecked) {
                logInfo("LSUI: Added tweaks, animation scale set to 0.7", findContext())
                runCommand(arrayOf(
                        "settings put global transition_animation_scale 0.7",
                        "settings put global animator_duration_scale 0.7",
                        "settings put global window_animation_scale 0.7",
                        "setprop persist.service.lgospd.enable 0",
                        "setprop persist.service.pcsync.enable 0",
                        "setprop touch.pressure.scale 0.001"))
                Snackbar.make(lsUi, R.string.liquid_on, Snackbar.LENGTH_SHORT).show()
            } else {
                runCommand(arrayOf(
                        "settings put global transition_animation_scale 1.0",
                        "settings put global animator_duration_scale 1.0",
                        "settings put global window_animation_scale 1.0",
                        "mount -o remount,rw /data",
                        "rm -f /data/property/persist.service.pcsync.enable",
                        "rm -f /data/property/persist.service.lgospd.enable",
                        "mount -o remount,ro /data"))
                logInfo("LSUI: Removed tweaks, animation scale set to 1.0", context)
                Snackbar.make(performanceTweak, R.string.liquid_off, Snackbar.LENGTH_SHORT).show()
            }
        })

        rolar.setOnCheckedChangeListener(null)
        rolar.setChecked(prefs.getBoolean(K.PREF.PERFORMANCE_SCROLLING, false))
        rolar.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener{ _, isChecked ->
            prefs.putBoolean(K.PREF.PERFORMANCE_SCROLLING, isChecked)
            if (isChecked) {
                logInfo("Added scrolling tweaks", context)
                Snackbar.make(lsUi, R.string.scroll_on, Snackbar.LENGTH_SHORT).show()
                RootUtils.runInternalScriptAsync("ro_on", findContext())
            } else {
                logInfo("Removed scrolling tweaks", findContext())
                Snackbar.make(performanceTweak, R.string.scroll_off, Snackbar.LENGTH_SHORT).show()
            }
        })

        fps.setOnCheckedChangeListener(null)
        fps.setChecked(prefs.getBoolean(K.PREF.PERFORMANCE_FPS_UNLOCKER, false))
        fps.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener{ _, isChecked ->
            prefs.putBoolean(K.PREF.PERFORMANCE_FPS_UNLOCKER, isChecked)
            if (isChecked) {
                Snackbar.make(fps, R.string.on, Snackbar.LENGTH_SHORT).show()
                runCommand("setprop debug.egl.swapinterval -60")
            } else {
                Snackbar.make(fps, R.string.off, Snackbar.LENGTH_SHORT).show()
                runCommand("setprop debug.egl.swapinterval 1")
            }
        })
    }

    private fun logInfo(info: String, context: Context?) {
        Logger.logInfo(info, context)
    }

    private fun bindViews(view: View) {
        gpuTweaks = view.findViewById(R.id.gpu)
        renderingTweaks = view.findViewById(R.id.render)
        callRingDelay = view.findViewById(R.id.ring)
        performanceTweak = view.findViewById(R.id.performance)
        multitasking = view.findViewById(R.id.multitasking)
        lsUi = view.findViewById(R.id.liquid)
        rolar = view.findViewById(R.id.scroll)
        fps = view.findViewById(R.id.fps)
    }

}