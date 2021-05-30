package com.androidvip.hebf.ui.base

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.R
import com.androidvip.hebf.createVectorDrawable
import com.androidvip.hebf.utils.*
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

abstract class BaseActivity: AppCompatActivity() {
    val workerContext: CoroutineContext = Dispatchers.Default + SupervisorJob()
    protected val prefs: Prefs by lazy { Prefs(applicationContext) }
    protected val userPrefs: UserPrefs by lazy { UserPrefs(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utils.toEnglish(this)
    }

    protected fun setUpToolbar(toolbar: Toolbar) {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(createVectorDrawable(R.drawable.ic_arrow_back))
        }
    }

    protected suspend fun isRooted() = withContext(workerContext) {
        return@withContext Shell.rootAccess()
    }

    suspend inline fun runSafeOnUiThread(crossinline uiBlock: () -> Unit) {
        if (!isFinishing) {
            withContext(Dispatchers.Main) {
                runCatching(uiBlock)
            }
        }
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

    protected val isAlive: Boolean get() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            !isFinishing && !isDestroyed
        } else !isFinishing
    }

    protected val isLandscape: Boolean get() = resources.getBoolean(R.bool.is_landscape)
    protected val isTablet: Boolean get() = resources.getBoolean(R.bool.is_tablet)
}