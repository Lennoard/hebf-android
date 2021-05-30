package com.androidvip.hebf.ui.main

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.androidvip.hebf.*
import com.androidvip.hebf.databinding.ActivityMain2Binding
import com.androidvip.hebf.helpers.HebfApp
import com.androidvip.hebf.services.RootShellService
import com.androidvip.hebf.ui.base.binding.BaseViewBindingActivity
import com.androidvip.hebf.ui.internal.AboutActivity
import com.androidvip.hebf.ui.internal.SplashActivity
import com.androidvip.hebf.ui.main.notif.NotificationsFragment
import com.androidvip.hebf.ui.main.tools.cleaner.CleanerActivity
import com.androidvip.hebf.ui.main.tune.cpu.CpuManagerActivity
import com.androidvip.hebf.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class MainActivity2 : BaseViewBindingActivity<ActivityMain2Binding>(ActivityMain2Binding::inflate) {
    private val animViewModel: LottieAnimViewModel by viewModel()
    private val notificationViewModel: NotificationViewModel by viewModel()
    private val handler by lazy { Handler(mainLooper) }
    private var actionMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            if (isRooted()) {
                startService(Intent(this@MainActivity2, RootShellService::class.java))
            }
        }

        with(binding.toolbar) {
            setSupportActionBar(this)
            ViewCompat.setElevation(this, 0F)
            supportActionBar?.setHomeAsUpIndicator(createVectorDrawable(R.drawable.ic_arrow_back))
        }

        val host = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val defaultIds = setOf(
            R.id.navigationDashboard,
            R.id.navigationBattery,
            R.id.navigationPerformance,
            R.id.navigationTools,
            R.id.navigationTune
        )
        val appBarConfiguration = AppBarConfiguration(defaultIds)

        animViewModel.animRes.observe(this) {
            binding.animation.apply {
                if (it == 0) {
                    hide()
                } else {
                    setAnimation(it)
                    show()
                    playAnimation()
                    this@MainActivity2.handler.removeCallbacks(lottieRunnable)
                    this@MainActivity2.handler.postDelayed(lottieRunnable,2000)
                }
            }
        }

        notificationViewModel.notificationCount.observe(this) {
            actionMenu?.findItem(R.id.actionNotifications)?.let {
                setupBadge(it)
            }
        }

        binding.collapsingLayout.setupWithNavController(
            binding.toolbar, host.navController, appBarConfiguration
        )
        binding.navView.setupWithNavController(host.navController)

        host.navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id !in defaultIds) {
                supportActionBar?.setHomeAsUpIndicator(
                    createVectorDrawable(R.drawable.ic_arrow_back)
                )
            }
        }

        setUpShortcuts()

        Logger.logDebug("Main screen displayed", this)
        Logger.logDebug("Checking for app crashes", this)
        checkForCrashes()

        if (prefs.getInt("versionCode", 1) < BuildConfig.VERSION_CODE) {
            prefs.putInt("versionCode", BuildConfig.VERSION_CODE)
            onUpdate()
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(lottieRunnable)
    }

    override fun onResume() {
        super.onResume()
        val app = application as HebfApp
        if (app.hasAppliedNewWmSetting()) {
            app.setAppliedNewWmSettings(false)
            handler.postDelayed({
                runCommand("wm density reset && wm size reset")
            }, 3000)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        actionMenu = menu
        menuInflater.inflate(R.menu.action_main, menu)
        val notificationsMenuItem = menu.findItem(R.id.actionNotifications)
        setupBadge(notificationsMenuItem)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionNotifications -> {
                findNavController(R.id.navHostFragment).navigate(R.id.navigationNotifications)
            }
            R.id.actionSettings -> {
                findNavController(R.id.navHostFragment).navigate(R.id.navigationPrefs)
            }
            R.id.actionDeviceInfo -> {
                findNavController(R.id.navHostFragment).navigate(R.id.navigationDeviceInfo)
            }
            R.id.actionMyAccount -> {
                findNavController(R.id.navHostFragment).navigate(R.id.navigationMyAccount)
            }
            R.id.actionAbout -> startActivity(Intent(this, AboutActivity::class.java))
        }
        return true
    }

    private fun setupBadge(item: MenuItem) {
        item.actionView?.setOnClickListener {
            onOptionsItemSelected(item)
        }

        lifecycleScope.launch(workerContext) {
            var notificationCount = 0

            val isBusyboxAvailable = RootUtils.executeSync("which busybox").isNotEmpty()
            val isRooted = isRooted()

            if (!isBusyboxAvailable && isRooted) {
                notificationCount++
            }

            if (!userPrefs.getBoolean(NotificationsFragment.PREF_HELP_ANDRE, false)) {
                notificationCount++
            }

            runSafeOnUiThread {
                item.actionView?.findViewById<AppCompatTextView>(R.id.badge)?.apply {
                    if (notificationCount > 0) {
                        show()
                        text = notificationCount.toString()
                    } else {
                        goAway()
                    }
                }
            }
        }
    }

    private fun setUpShortcuts() {
        val isNougat = Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = getSystemService(ShortcutManager::class.java)
            val shortcutInfos = ArrayList<ShortcutInfo>()
            shortcutInfos.add(
                ShortcutInfo.Builder(this, K.SHORTCUT_ID_BATTERY)
                .setShortLabel(getString(R.string.battery))
                .setIcon(Icon.createWithResource(
                    this, if (isNougat) R.drawable.ic_shortcut_battery else R.mipmap.ic_shortcut_battery)
                )
                .setIntent(buildShortcutIntent(SplashActivity::class.java, K.SHORTCUT_ID_BATTERY))
                .build())
            shortcutInfos.add(
                ShortcutInfo.Builder(this, K.SHORTCUT_ID_PERFORMANCE)
                .setShortLabel(getString(R.string.performance))
                .setIcon(Icon.createWithResource(
                    this, if (isNougat) R.drawable.ic_shortcut_performance else R.mipmap.ic_shortcut_performance)
                )
                .setIntent(buildShortcutIntent(SplashActivity::class.java, K.SHORTCUT_ID_PERFORMANCE))
                .build())
            shortcutInfos.add(
                ShortcutInfo.Builder(this, K.SHORTCUT_ID_CLEANER)
                .setShortLabel(getString(R.string.cleaner))
                .setIcon(Icon.createWithResource(
                    this, if (isNougat) R.drawable.ic_shortcut_cleaner else R.mipmap.ic_shortcut_cleaner)
                )
                .setIntent(buildShortcutIntent(CleanerActivity::class.java, K.SHORTCUT_ID_CLEANER))
                .build())
            shortcutInfos.add(
                ShortcutInfo.Builder(this, K.SHORTCUT_ID_CPU)
                .setShortLabel(getString(R.string.cpu_manager))
                .setIcon(Icon.createWithResource(
                    this, if (isNougat) R.drawable.ic_shortcut_cpu else R.mipmap.ic_shortcut_cpu)
                )
                .setIntent(buildShortcutIntent(CpuManagerActivity::class.java, K.SHORTCUT_ID_CPU))
                .build())

            shortcutManager.dynamicShortcuts = shortcutInfos
        }
    }

    private fun buildShortcutIntent(cls: Class<*>, extra: String): Intent {
        return Intent().setAction(Intent.ACTION_VIEW)
            .setPackage(BuildConfig.APPLICATION_ID)
            .setClass(applicationContext, cls)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(K.EXTRA_SHORTCUT_ID, extra)
    }

    private fun switchFragmentFromIntent() {
        val shortcutId = intent.getStringExtra(K.EXTRA_SHORTCUT_ID)
        if (shortcutId != null) {
            when (shortcutId) {
                K.SHORTCUT_ID_BATTERY -> {
                    findNavController(R.id.navHostFragment).navigate(R.id.navigationBattery)
                }
                K.SHORTCUT_ID_PERFORMANCE -> {
                    findNavController(R.id.navHostFragment).navigate(R.id.navigationPerformance)
                }
                else -> {
                    findNavController(R.id.navHostFragment).navigate(R.id.navigationDashboard)
                }
            }
        }
    }

    private fun checkForCrashes() {
        val crashed = userPrefs.getBoolean(K.PREF.HAS_CRASHED, false)
        val msg = userPrefs.getString(K.PREF.CRASH_MESSAGE, "System died").take(5000)

        if (crashed) {
            MaterialAlertDialogBuilder(this)
                .setCancelable(true)
                .setTitle("Ops :(")
                .setMessage(R.string.crash_info)
                .setOnDismissListener { userPrefs.putBoolean(K.PREF.HAS_CRASHED, false) }
                .also {
                    if (BuildConfig.DEBUG) {
                        it.setNeutralButton("View") { dialog, _ ->
                            userPrefs.putBoolean(K.PREF.HAS_CRASHED, false)
                            dialog.dismiss()
                            MaterialAlertDialogBuilder(this)
                                .setTitle("Crash log")
                                .setMessage(msg)
                                .setNegativeButton(getString(R.string.close)) { _, _ -> }
                                .applyAnim().also {
                                    it.show()
                                }
                        }
                    }
                }
                .setNegativeButton(getString(R.string.close)) { _, _ -> }
                .setPositiveButton(getString(R.string.send)) { _, _ ->
                    userPrefs.putBoolean(K.PREF.HAS_CRASHED, false)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "message/rfc822"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("support@androidvip.com.br"))
                        putExtra(Intent.EXTRA_SUBJECT, "HEBF Optimizer")
                        putExtra(Intent.EXTRA_TEXT, msg)
                    }
                    try {
                        startActivity(Intent.createChooser(intent, "Send mail..."))
                        toast("Choose your email client")
                    } catch (ex: ActivityNotFoundException) {
                        Logger.logWTF("There are no email clients installed! ${ex.message}", this)
                    } catch (ex: Exception) {
                        Logger.logError(ex, this)
                    }
                }.applyAnim().also {
                    it.show()
                }
        } else {
            Logger.logDebug("No crash found", this)
        }
    }

    private fun onUpdate() {
        userPrefs.putBoolean(K.PREF.INFO_SHOWN, false)
        Utils.copyAssets(this)
        showInfo(getString(R.string.changelog) + getString(R.string.build_changelog))
    }

    private fun showInfo(message: String) {
        val infoShown = userPrefs.getBoolean(K.PREF.INFO_SHOWN, false)
        if (!infoShown) {
            MaterialAlertDialogBuilder(this@MainActivity2)
                .setTitle(getString(R.string.updated))
                .setIcon(getColoredVectorDrawable(R.drawable.ic_info_outline))
                .setMessage(message)
                .setOnDismissListener {
                    userPrefs.putBoolean(K.PREF.INFO_SHOWN, true)
                }.applyAnim().also {
                    it.show()
                }
        }
    }

    private val lottieRunnable = Runnable {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            binding.animation.hide()
        }
    }
}