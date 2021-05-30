package com.androidvip.hebf.ui.main.performance

import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.R
import com.androidvip.hebf.getThemedVectorDrawable
import com.androidvip.hebf.helpers.GamePackages
import com.androidvip.hebf.toast
import com.androidvip.hebf.ui.base.BaseActivity
import com.androidvip.hebf.utils.*
import com.androidvip.hebf.utils.gb.GameBoosterImpl
import com.androidvip.hebf.utils.gb.GameBoosterNutellaImpl
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.topjohnwu.superuser.Shell
import kotlinx.android.synthetic.main.activity_running_apps.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LaunchGamesActivity : BaseActivity() {
    private val packagesManager: PackagesManager by lazy { PackagesManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_running_apps)

        setUpToolbar(toolbar)

        lifecycleScope.launch {
            val installedGames = getInstalledGames()

            runningAppsRecyclerView.layoutManager = GridLayoutManager(
                    this@LaunchGamesActivity, 3
            )
            runningAppsRecyclerView.setHasFixedSize(true)
            runningAppsRecyclerView.adapter = LaunchAppsAdapter(
                    this@LaunchGamesActivity, getInstalledGames()
            )

            if (installedGames.isEmpty()) {
                Snackbar.make(runningAppsRecyclerView, "No game found!", Snackbar.LENGTH_INDEFINITE).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return true
    }

    private suspend fun getInstalledGames(): List<String> = withContext(Dispatchers.Default) {
        val resultList = mutableListOf<String>()
        val installedPackages = packagesManager.installedPackages
        val legitGames = installedPackages.filter { packageIsGame(it) }

        GamePackages.gamePackagesList.forEach {
            if (it in installedPackages) {
                resultList.add(it)
            }
        }

        return@withContext (legitGames + resultList).distinct().sorted().toMutableList().apply {
            add("otherGame")
        }
    }

    private fun packageIsGame(packageName: String): Boolean {
        return try {
            val info: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                info.category == ApplicationInfo.CATEGORY_GAME
            } else {
                @Suppress("DEPRECATION")
                (info.flags and ApplicationInfo.FLAG_IS_GAME) == ApplicationInfo.FLAG_IS_GAME
            }
        } catch (e: Exception) {
            false
        }
    }

    private inner class LaunchAppsAdapter(
        private val activity: BaseActivity,
        private val dataSet: List<String>
    ) : RecyclerView.Adapter<LaunchAppsAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            var label: TextView = v.findViewById(R.id.appName)
            var icon: ImageView = v.findViewById(R.id.appIcon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(activity).inflate(R.layout.list_item_launch_app, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val pckg = dataSet[position]

            with (holder) {
                if (pckg == "otherGame") {
                    label.text = getString(R.string.other)
                    icon.setImageDrawable(getThemedVectorDrawable(R.drawable.ic_android))
                    itemView.setOnClickListener {
                        toast(R.string.loading, false)
                        showPickAppsDialog()
                    }
                } else {
                    label.text = packagesManager.getAppLabel(pckg)
                    icon.setImageDrawable(packagesManager.getAppIcon(pckg))

                    itemView.setOnClickListener {
                        launchApp(pckg)
                    }
                }
            }
        }

        private fun launchApp(pckg: String) {
            lifecycleScope.launch(workerContext) {
                val isRooted = Shell.rootAccess()
                if (isRooted) {
                    GameBoosterImpl(activity).enable()
                } else {
                    GameBoosterNutellaImpl(activity).enable()
                }

                runSafeOnUiThread {
                    try {
                        startActivity(packageManager.getLaunchIntentForPackage(pckg))
                    } catch (e: Exception) {
                        Logger.logWTF("Failed to launch $pckg", activity)
                    }
                }
            }
        }

        private fun showPickAppsDialog() {
            val builder = MaterialAlertDialogBuilder(this@LaunchGamesActivity)

            lifecycleScope.launch(workerContext) {
                val packages = packagesManager.installedPackages
                val apps = mutableListOf<Pair<String, String>>()

                for (packageName in packages) {
                    apps.add(packageName to packagesManager.getAppLabel(packageName))
                }

                apps.sortBy { it.second }
                builder.setItems(apps.map { it.second }.toTypedArray()) { _, which ->
                    launchApp(apps[which].first)
                }

                runSafeOnUiThread {
                    builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
                    builder.show()
                }
            }
        }

        override fun getItemCount(): Int {
            return dataSet.size
        }
    }
}