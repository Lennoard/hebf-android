package com.androidvip.hebf.ui.base

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
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

abstract class BaseFragment : Fragment() {
    protected val workerContext: CoroutineContext = Dispatchers.Default + SupervisorJob()
    protected val prefs: Prefs by lazy { Prefs(applicationContext) }
    protected val userPrefs: UserPrefs by lazy { UserPrefs(applicationContext) }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        recreate()
    }

    protected fun findContext(): Context {
        if (activity != null) return requireActivity()
        return requireContext()
    }

    protected suspend fun isRooted() = withContext(workerContext) {
        return@withContext Shell.rootAccess()
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