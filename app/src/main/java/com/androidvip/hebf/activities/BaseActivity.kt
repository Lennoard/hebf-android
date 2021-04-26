package com.androidvip.hebf.activities

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import com.androidvip.hebf.R
import com.androidvip.hebf.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

abstract class BaseActivity : AppCompatActivity() {
    val workerContext: CoroutineContext = Dispatchers.Default + SupervisorJob()
    protected val prefs: Prefs by lazy { Prefs(applicationContext) }
    protected val userPrefs: UserPrefs by lazy { UserPrefs(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utils.toEnglish(this)
    }

    protected fun setUpToolbar(toolbar: Toolbar) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    suspend inline fun runSafeOnUiThread(crossinline uiBlock: () -> Unit) {
        if (!isFinishing) {
            withContext(Dispatchers.Main) {
                whenStarted {
                    runCatching(uiBlock)
                }
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

    protected val isAlive: Boolean
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                !isFinishing && !isDestroyed
            } else !isFinishing
        }

    protected val isLandscape: Boolean get() = resources.getBoolean(R.bool.is_landscape)
    protected val isTablet: Boolean get() = resources.getBoolean(R.bool.is_tablet)
}