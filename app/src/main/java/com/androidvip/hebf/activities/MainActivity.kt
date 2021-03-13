package com.androidvip.hebf.activities

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.BuildConfig
import com.androidvip.hebf.R
import com.androidvip.hebf.activities.internal.AboutActivity
import com.androidvip.hebf.fragments.*
import com.androidvip.hebf.getThemedVectorDrawable
import com.androidvip.hebf.helpers.HebfApp
import com.androidvip.hebf.services.RootShellService
import com.androidvip.hebf.toast
import com.androidvip.hebf.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.topjohnwu.superuser.Shell
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUpToolbar(toolbar)

        Utils.setExceptionHandler(this)

        Logger.logDebug("Main screen displayed", this)

        setUpShortcuts()
        setUpDrawer(toolbar)

        Logger.logDebug("Checking for app crashes", this)
        checkForCrashes()

        if (prefs.getInt("versionCode", 1) < BuildConfig.VERSION_CODE) {
            prefs.putInt("versionCode", BuildConfig.VERSION_CODE)
            onUpdate()
        }

        switchFragmentFromIntent()
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch(workerContext) {
            val isRootAccessGiven = Shell.rootAccess()
            runSafeOnUiThread {
                if (isRootAccessGiven) {
                    userPrefs.putBoolean(K.PREF.USER_HAS_ROOT, true)
                    Logger.logDebug("Starting root shell", this@MainActivity)
                    startRootShell()
                } else {
                    toast("Root access not found or denied by superuser app!", false)
                    userPrefs.putBoolean(K.PREF.USER_HAS_ROOT, false)
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val app = applicationContext as HebfApp
        if (app.hasAppliedNewWmSetting()) {
            app.setAppliedNewWmSettings(false)
            Handler().postDelayed({ runCommand("wm density reset && wm size reset") }, 3000)
        }
    }

    // Fixes Lollipop WebView crash in androidx.appcompat:appcompat:1.1.0
    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (Build.VERSION.SDK_INT in 21..25 && (resources.configuration.uiMode == applicationContext.resources.configuration.uiMode)) {
            return
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onBackPressed() {
        fun showCloseDialog() {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.sair)
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    stopService(Intent(this, RootShellService::class.java))
                    finish()
                }
                .setNegativeButton(android.R.string.no) { _, _ -> }.show()
        }

        when {
            isTablet && isLandscape -> {
                showCloseDialog()
            }
            drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            else -> {
                showCloseDialog()
            }
        }

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> Utils.replaceFragment(DashboardFragment2(), this, getString(R.string.app_name))
            R.id.nav_info -> Utils.replaceFragment(DeviceInfoFragment(), this, getString(R.string.device_info))
            R.id.nav_fstrim -> Utils.replaceFragment(FstrimFragment(), this, getString(R.string.fstrim))
            R.id.nav_bateria -> Utils.replaceFragment(BatteryFragment2(), this, getString(R.string.battery))
            R.id.nav_performance -> Utils.replaceFragment(PerformanceFragment(), this, getString(R.string.performance))
            R.id.nav_game -> Utils.replaceFragment(GameBoosterFragment(), this, "Game Booster")
            R.id.nav_vip -> Utils.replaceFragment(VipFragment(), this, getString(R.string.vip_battery_saver))
            R.id.nav_net -> Utils.replaceFragment(NetFragment(), this, getString(R.string.internet_tweaks))
            R.id.nav_advanced -> Utils.replaceFragment(AdvancedFragment(), this, getString(R.string.advanced_options))
            R.id.nav_tools -> Utils.replaceFragment(ToolsFragment(), this, getString(R.string.tools))
            R.id.nav_ram_manager -> Utils.replaceFragment(RamManagerFragment(), this, getString(R.string.ram_manager))
            R.id.nav_cpu -> {
                Handler().postDelayed(Runnable {
                    startActivity(Intent(this, CpuManagerActivity::class.java))
                    overridePendingTransition(R.anim.fragment_open_enter, R.anim.fragment_open_exit)
                }, 200)
            }
            R.id.nav_kernel -> Utils.replaceFragment(KernelOptionsFragment(), this, getString(R.string.kernel))
            R.id.nav_cleaner -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Utils.hasStoragePermissions(this)) {
                        Utils.showStorageSnackBar(this, mainCl)

                        val perm = MaterialAlertDialogBuilder(this)
                        perm.setIcon(getThemedVectorDrawable(R.drawable.ic_warning))
                        perm.setTitle("Ops")
                        perm.setMessage(getString(R.string.request_storage_permission_warning) + "\n"
                            + getString(R.string.screen_overlay_sub))
                        perm.setNegativeButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }.show()
                    } else {
                        Handler().postDelayed(Runnable {
                            startActivity(Intent(this, CleanerActivity::class.java))
                            overridePendingTransition(R.anim.fragment_open_enter, R.anim.fragment_open_exit)
                        }, 200)
                    }
                } else {
                    Handler().postDelayed(Runnable {
                        startActivity(Intent(this, CleanerActivity::class.java))
                        overridePendingTransition(R.anim.fragment_open_enter, R.anim.fragment_open_exit)
                    }, 200)
                }
            }
            R.id.nav_settings -> Utils.replaceFragment(PreferencesFragment(), this, getString(R.string.settings))
            R.id.nav_about_less -> {
                Handler().postDelayed(Runnable {
                    startActivity(Intent(this, AboutActivity::class.java))
                    overridePendingTransition(R.anim.fragment_open_enter, R.anim.fragment_open_exit)
                }, 256)
            }
        }

        if (!isLandscape || !isTablet) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        return true
    }

    private fun startRootShell() {
        val context = this@MainActivity
        val activity = this@MainActivity

        val success = AtomicBoolean(true)
        lifecycleScope.launch(workerContext) {
            val builder = MaterialAlertDialogBuilder(context)
                .setTitle(R.string.error)
                .setIcon(getThemedVectorDrawable(R.drawable.ic_warning))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ -> Utils.killMe(activity) }

            try {
                success.set(Shell.rootAccess())
            } catch (e: Exception) {
                builder.setMessage("Unable to open root shell, ${e.message}")
                builder.setNeutralButton("Try again") { _, _ -> startRootShell() }
                success.set(false)
            }

            runSafeOnUiThread {
                if (!success.get()) {
                    builder.show()
                } else {
                    startService(Intent(activity, RootShellService::class.java))
                }
            }
        }
    }

    private fun setUpShortcuts() {
        val isNougat = Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = getSystemService(ShortcutManager::class.java)
            val shortcutInfos = ArrayList<ShortcutInfo>()
            shortcutInfos.add(ShortcutInfo.Builder(this, K.SHORTCUT_ID_BATTERY)
                .setShortLabel(getString(R.string.battery))
                .setIcon(Icon.createWithResource(this, if (isNougat) R.drawable.ic_shortcut_battery else R.mipmap.ic_shortcut_battery))
                .setIntent(buildShortcutIntent(SplashActivity::class.java, K.SHORTCUT_ID_BATTERY))
                .build())
            shortcutInfos.add(ShortcutInfo.Builder(this, K.SHORTCUT_ID_PERFORMANCE)
                .setShortLabel(getString(R.string.performance))
                .setIcon(Icon.createWithResource(this, if (isNougat) R.drawable.ic_shortcut_performance else R.mipmap.ic_shortcut_performance))
                .setIntent(buildShortcutIntent(SplashActivity::class.java, K.SHORTCUT_ID_PERFORMANCE))
                .build())
            shortcutInfos.add(ShortcutInfo.Builder(this, K.SHORTCUT_ID_CLEANER)
                .setShortLabel(getString(R.string.cleaner))
                .setIcon(Icon.createWithResource(this, if (isNougat) R.drawable.ic_shortcut_cleaner else R.mipmap.ic_shortcut_cleaner))
                .setIntent(buildShortcutIntent(CleanerActivity::class.java, K.SHORTCUT_ID_CLEANER))
                .build())
            shortcutInfos.add(ShortcutInfo.Builder(this, K.SHORTCUT_ID_CPU)
                .setShortLabel(getString(R.string.cpu_manager))
                .setIcon(Icon.createWithResource(this, if (isNougat) R.drawable.ic_shortcut_cpu else R.mipmap.ic_shortcut_cpu))
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
                    Utils.replaceFragment(BatteryFragment2(), this, getString(R.string.battery))
                    navigationView.setCheckedItem(R.id.nav_bateria)
                }
                K.SHORTCUT_ID_PERFORMANCE -> {
                    Utils.replaceFragment(PerformanceFragment(), this, getString(R.string.performance))
                    navigationView.setCheckedItem(R.id.nav_performance)
                }
                else -> {
                    Utils.replaceFragment(DashboardFragment2(), this, getString(R.string.dashboard))
                    navigationView.setCheckedItem(R.id.nav_dashboard)
                }
            }
        } else {
            Utils.replaceFragment(DashboardFragment2(), this, getString(R.string.dashboard))
            navigationView.setCheckedItem(R.id.nav_dashboard)
        }
    }

    private fun setUpDrawer(toolbar: Toolbar) {
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )

        if (userPrefs.getString(K.PREF.THEME, Themes.LIGHT) == Themes.WHITE) {
            toggle.isDrawerIndicatorEnabled = false
            toggle.setHomeAsUpIndicator(R.drawable.ic_menu_white_theme)
            toggle.toolbarNavigationClickListener = View.OnClickListener {

                when {
                    isLandscape && isTablet -> {
                        onBackPressed()
                    }
                    drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    }
                    else -> {
                        drawerLayout.openDrawer(GravityCompat.START)
                    }
                }
            }
        }

        if (isLandscape && isTablet) {
            drawerLayout.openDrawer(GravityCompat.START)
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)
        } else {
            drawerLayout.addDrawerListener(toggle)
        }

        toggle.syncState()
        navigationView.setCheckedItem(R.id.nav_dashboard)
        navigationView.setNavigationItemSelectedListener(this)

        navigationView.menu.clear()
        navigationView.inflateMenu(R.menu.drawer_advanced)
    }

    private fun checkForCrashes() {
        val crashed = userPrefs.getBoolean(K.PREF.HAS_CRASHED, false)
        var msg = userPrefs.getString(K.PREF.CRASH_MESSAGE, "System died")
        if (msg.length > 5000) {
            msg = msg.substring(0..4999)
        }

        if (crashed) {
            MaterialAlertDialogBuilder(this)
                .setCancelable(true)
                .setTitle("Ops :(")
                .setMessage(R.string.crash_info)
                .setOnDismissListener { userPrefs.putBoolean(K.PREF.HAS_CRASHED, false) }
                .setNeutralButton("View") { dialog, _ ->
                    userPrefs.putBoolean(K.PREF.HAS_CRASHED, false)
                    dialog.dismiss()
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Crash log")
                        .setMessage(msg)
                        .setNegativeButton(getString(R.string.close)) { _, _ -> }
                        .show()
                }
                .setNegativeButton(getString(R.string.close)) { _, _ -> }
                .show()
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
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(getString(R.string.updated))
                .setIcon(getThemedVectorDrawable(R.drawable.ic_info_outline))
                .setCancelable(false)
                .setMessage(message)
                .setNegativeButton(android.R.string.ok) { _, _ -> userPrefs.putBoolean(K.PREF.INFO_SHOWN, true) }
                .show()
        }
    }
}
