package com.androidvip.hebf.fragments

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.utils.Prefs
import com.androidvip.hebf.utils.RootUtils
import com.androidvip.hebf.utils.UserPrefs
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

open class BaseFragment : Fragment() {
    protected val workerContext: CoroutineContext = Dispatchers.Default + SupervisorJob()
    protected val prefs: Prefs by lazy { Prefs(requireContext().applicationContext) }
    protected val userPrefs: UserPrefs by lazy { UserPrefs(requireContext().applicationContext) }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        recreate()
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        var animation = super.onCreateAnimation(transit, enter, nextAnim)
        if (animation == null && nextAnim != 0) {
            animation = AnimationUtils.loadAnimation(requireContext(), nextAnim)
        }
        if (animation != null) {
            val v = view
            if (v != null) {
                v.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                animation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}
                    override fun onAnimationEnd(animation: Animation) {
                        v.setLayerType(View.LAYER_TYPE_NONE, null)
                    }

                    override fun onAnimationRepeat(animation: Animation) {}
                })
            }
        }
        return animation
    }

    protected fun findContext(): Context {
        if (activity != null) return requireActivity()
        return requireContext()
    }

    fun runCommand(command: String) {
        lifecycleScope.launch(workerContext) {
            RootUtils.execute(command)
        }
    }

    fun runCommand(array: Array<String>) {
        lifecycleScope.launch(workerContext) {
            RootUtils.execute(array)
        }
    }

    val isActivityAlive: Boolean
        get() = activity != null && !requireActivity().isFinishing && isAdded

    val applicationContext: Context
        get() = requireContext().applicationContext

    private fun recreate() {
        if (!isAdded) return

        parentFragmentManager
            .beginTransaction()
            .detach(this)
            .attach(this)
            .commit()
    }
}