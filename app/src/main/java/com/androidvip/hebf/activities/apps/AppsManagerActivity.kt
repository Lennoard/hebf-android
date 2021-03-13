package com.androidvip.hebf.activities.apps

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.R
import com.androidvip.hebf.activities.BaseActivity
import com.androidvip.hebf.getThemedVectorDrawable
import com.androidvip.hebf.models.App
import com.androidvip.hebf.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_apps_manager.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class AppsManagerActivity : BaseActivity() {
    private val packagesManager: PackagesManager by lazy { PackagesManager(this) }
    private var appsList: MutableList<App> = mutableListOf()
    private var tempList: MutableList<App> = mutableListOf()
    private lateinit var appsAdapter: RecyclerView.Adapter<*>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apps_manager)

        setUpToolbar(toolbar)

        val typedValueAccent = TypedValue()
        theme.resolveAttribute(R.attr.colorAccent, typedValueAccent, true)
        appsManagerSwipeLayout.setColorSchemeColors(ContextCompat.getColor(this, typedValueAccent.resourceId))
        appsManagerSwipeLayout.setOnRefreshListener { refreshList(currentFilter) }
        appsManagerSwipeLayout.isRefreshing = true

        appsManagerRecyclerView.setHasFixedSize(true)
        appsManagerRecyclerView.layoutManager = LinearLayoutManager(this)
        appsManagerRecyclerView.addItemDecoration(DividerItemDecoration(appsManagerRecyclerView.context, RecyclerView.VERTICAL))

        Logger.logDebug("Listing installed applications", this)
        lifecycleScope.launch(workerContext) {
            appsList = packagesManager.getThirdPartyApps()
            appsAdapter = AppsAdapter(this@AppsManagerActivity, appsList)

            runSafeOnUiThread {
                appsManagerRecyclerView.adapter = appsAdapter
                appsManagerSwipeLayout.isRefreshing = false
            }
        }
    }

    private fun filterListByMatch(query: String): MutableList<App> {
        return appsList.filter { app ->
            app.label.toLowerCase(Locale.getDefault()).contains(query.toLowerCase(Locale.getDefault()))
        }.toMutableList()
    }

    private fun refreshList(filterMode: String) {
        appsManagerSwipeLayout.isRefreshing = true
        lifecycleScope.launch(workerContext) {
            when (filterMode) {
                PackagesManager.FILTER_THIRD_PARTY -> appsList = packagesManager.getThirdPartyApps()
                PackagesManager.FILTER_DISABLED -> appsList = packagesManager.getDisabledApps()
                "" -> appsList = packagesManager.getAllPackages()
            }

            runSafeOnUiThread {
                appsAdapter = AppsAdapter(this@AppsManagerActivity, appsList)
                appsManagerRecyclerView.adapter = appsAdapter
                appsManagerSwipeLayout.isRefreshing = false
            }
        }
    }

    private fun updateListWithFilter(listOption: String) {
        appsManagerSwipeLayout.isRefreshing = true
        tempList.clear()

        lifecycleScope.launch(workerContext) {
            try {
                when (listOption) {
                    PackagesManager.FILTER_THIRD_PARTY -> {
                        currentFilter = PackagesManager.FILTER_THIRD_PARTY
                        tempList.addAll(packagesManager.getThirdPartyApps())
                    }
                    PackagesManager.FILTER_SYSTEM -> {
                        currentFilter = PackagesManager.FILTER_SYSTEM
                        tempList.addAll(packagesManager.getSystemApps())
                    }
                    PackagesManager.FILTER_DISABLED -> {
                        currentFilter = PackagesManager.FILTER_DISABLED
                        tempList.addAll(packagesManager.getDisabledApps())
                    }
                    "" -> {
                        currentFilter = ""
                        tempList.addAll(packagesManager.getAllPackages())
                    }
                }
                appsList.clear()
                appsList.addAll(tempList)

                runSafeOnUiThread {
                    tempList.clear()
                    appsAdapter = AppsAdapter(this@AppsManagerActivity, appsList)
                    appsManagerRecyclerView.adapter = appsAdapter
                    appsManagerSwipeLayout.isRefreshing = false
                }
            } catch (ignored: Exception) {
            }
        }
    }

    private fun startDetailActivity(app: App) {
        val intent = Intent(this, AppDetailsActivity::class.java)
        intent.putExtra(K.EXTRA_APP, app)
        startActivity(intent)
    }

    private fun disablePackage(packageName: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.warning))
            .setIcon(getThemedVectorDrawable(R.drawable.ic_warning))
            .setMessage(getString(R.string.confirmation_message))
            .setNegativeButton(R.string.cancelar) { _, _ -> }
            .setPositiveButton(R.string.disable) { _, _ ->
                runCommand("pm disable $packageName")
                Snackbar.make(appsManagerCl, R.string.package_disabled, Snackbar.LENGTH_INDEFINITE)
                    .setAction("OK") { refreshList(currentFilter) }
                    .show()
                Logger.logInfo("Package disabled: $packageName", this)
            }
            .show()
    }

    private fun uninstallApp(app: App) {
        if (app.isSystemApp) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.warning))
                .setIcon(getThemedVectorDrawable(R.drawable.ic_warning))
                .setMessage(getString(R.string.confirmation_message))
                .setNegativeButton(R.string.cancelar) { _, _ -> }
                .setPositiveButton(R.string.uninstall) { _, _ ->
                    lifecycleScope.launch(workerContext) {
                        // Get package path
                        val packagePathString = RootUtils.executeWithOutput(
                            "pm path ${app.packageName}"
                        ).substring(8)
                        val packagePath = File(packagePathString)
                        if (packagePath.exists()) {
                            RootUtils.deleteFileOrDir(packagePathString)
                            RootUtils.deleteFileOrDir("${Environment.getDataDirectory()}/data/${app.packageName}")
                            Logger.logInfo("Deleted package: ${app.packageName}", this@AppsManagerActivity)
                            if (!isFinishing) {
                                runSafeOnUiThread {
                                    MaterialAlertDialogBuilder(this@AppsManagerActivity)
                                        .setTitle(R.string.package_uninstalled)
                                        .setMessage("Reboot your device")
                                        .setPositiveButton(android.R.string.ok) { _, _ -> }
                                        .setNeutralButton(R.string.reboot) { _, _ -> runCommand("reboot") }
                                        .show()
                                }
                            }
                        } else {
                            runSafeOnUiThread {
                                Logger.logWTF("Tried to delete dir of ${app.packageName} but found nothing there", this@AppsManagerActivity)
                                Snackbar.make(appsManagerCl, "Error: $packagePath does not exist", Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }.start()
                }
                .show()
        } else {
            Logger.logInfo("Attempting to uninstall ${app.label} (${app.packageName})", this@AppsManagerActivity)
            try {
                val packageURI = Uri.parse("package:$app")
                val uninstallIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI)
                startActivity(uninstallIntent)
            } catch (e: Exception) {
                Logger.logError("Could not uninstall package: ${app.packageName}. ${e.localizedMessage}", this@AppsManagerActivity)
                Toast.makeText(this, "Could not launch uninstall dialog for package: ${app.packageName}. Reason: ${e.message}", Toast.LENGTH_LONG).show()
            }

        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.apps_manager, menu)
        if (UserPrefs(applicationContext).getString(K.PREF.THEME, Themes.LIGHT) == Themes.WHITE) {
            for (i in 0 until menu.size()) {
                val menuItem = menu.getItem(i)
                if (menuItem != null) {
                    val iconDrawable = menuItem.icon
                    if (iconDrawable != null) {
                        iconDrawable.mutate()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            iconDrawable.setTint(ContextCompat.getColor(this, R.color.colorAccentWhite))
                        } else {
                            iconDrawable.setColorFilter(ContextCompat.getColor(this, R.color.colorAccentWhite), PorterDuff.Mode.LIGHTEN)
                        }
                    }
                }
            }

        }
        val searchMenuItem = menu.findItem(R.id.action_search)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager?
        val searchView = searchMenuItem.actionView as SearchView
        searchView.queryHint = getString(R.string.search)
        if (searchManager != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
            searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String): Boolean {
                            appsAdapter = AppsAdapter(this@AppsManagerActivity, filterListByMatch(query))
                            appsManagerRecyclerView.adapter = appsAdapter
                            return true
                        }

                        override fun onQueryTextChange(newText: String): Boolean {
                            if (newText.length > 2) {
                                appsAdapter = AppsAdapter(this@AppsManagerActivity, filterListByMatch(newText))
                                appsManagerRecyclerView.adapter = appsAdapter
                                return true
                            } else if (newText.isEmpty()) {
                                appsAdapter = AppsAdapter(this@AppsManagerActivity, appsList)
                                appsManagerRecyclerView.adapter = appsAdapter
                                return true
                            }
                            return false
                        }
                    })
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    appsAdapter = AppsAdapter(this@AppsManagerActivity, appsList)
                    appsManagerRecyclerView.adapter = appsAdapter
                    return true
                }
            })
        }
        return true
    }

    override fun onBackPressed() {
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fragment_close_enter, android.R.anim.slide_out_right)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }

            R.id.action_filter -> {
                val builder = MaterialAlertDialogBuilder(this)
                val array = arrayOf<CharSequence>(
                    getString(R.string.apps_manager_3rd_party),
                    getString(R.string.apps_manager_system_apps),
                    getString(R.string.apps_manager_disabled_apps),
                    getString(R.string.apps_manager_all_apps)
                )
                builder.setTitle(R.string.choose_one_filter)
                    .setSingleChoiceItems(array, 0) { _, _ -> }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                        when ((dialog as AlertDialog).listView.checkedItemPosition) {
                            0 -> updateListWithFilter(PackagesManager.FILTER_THIRD_PARTY)
                            1 -> updateListWithFilter(PackagesManager.FILTER_SYSTEM)
                            2 -> updateListWithFilter(PackagesManager.FILTER_DISABLED)
                            3 -> updateListWithFilter("")
                        }
                    }
                builder.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private inner class AppsAdapter(private val context: Context, private val dataSet: MutableList<App>) : RecyclerView.Adapter<AppsAdapter.ViewHolder>() {

        init {
            dataSet.sortWith { one: App, other: App -> one.label.compareTo(other.label) }
        }

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            var itemLayout: RelativeLayout = v.findViewById(R.id.app_root_layout)

            var appName: TextView = v.findViewById(R.id.app_name)
            var appPackageName: TextView = v.findViewById(R.id.app_package_name)
            var appType: TextView = v.findViewById(R.id.app_type)
            var appStatus: TextView = v.findViewById(R.id.app_status)
            var appIcon: ImageView = v.findViewById(R.id.app_icon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(context).inflate(R.layout.list_item_apps_manager, parent, false)
            return ViewHolder(v)
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = dataSet[position]

            holder.appType.text = "user"
            holder.appStatus.text = getString(R.string.on)
            holder.appStatus.setTextColor(Color.parseColor("#9e9e9e"))

            holder.appName.text = app.label

            holder.appPackageName.text = app.packageName
            holder.appPackageName.isSingleLine = true
            holder.appPackageName.ellipsize = TextUtils.TruncateAt.MARQUEE
            holder.appPackageName.isSelected = true
            holder.appPackageName.marqueeRepeatLimit = 5

            holder.appIcon.setImageDrawable(app.icon)

            holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))

            if (app.isSystemApp)
                holder.appType.text = "system"

            if (!app.isEnabled) {
                holder.appStatus.text = getString(R.string.off)
                holder.appStatus.setTextColor(Color.parseColor("#ff5252"))
            }

            holder.itemLayout.setOnClickListener { startDetailActivity(app) }

            holder.itemLayout.setOnLongClickListener {
                val menuOptions = arrayListOf<SheetOption>()
                if (!app.isEnabled) {
                    menuOptions.add(SheetOption(getString(R.string.enable), "enable", R.drawable.ic_check))
                } else {
                    menuOptions.add(SheetOption(getString(R.string.disable), "disable", R.drawable.ic_perfis_1))
                    menuOptions.add(SheetOption(getString(R.string.launch), "launch", R.drawable.ic_play_arrow))
                }
                menuOptions.add(SheetOption(getString(R.string.uninstall), "uninstall", R.drawable.ic_delete))

                ContextBottomSheet.newInstance(app.label, menuOptions).apply {
                    onOptionClickListener = object : ContextBottomSheet.OnOptionClickListener {
                        override fun onOptionClick(tag: String) {
                            dismiss()
                            when (tag) {
                                "launch" -> {
                                    try {
                                        val launchIntent = context?.packageManager?.getLaunchIntentForPackage(app.packageName)
                                        context?.startActivity(launchIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not launch app: ${app.label}", Toast.LENGTH_SHORT).show()
                                        Logger.logError("Could not launch app: ${app.label}", context)
                                    }
                                }

                                "uninstall" -> if (app.packageName.contains("com.androidvip.hebf")) {
                                    Snackbar.make(appsManagerCl, "You are a comedian :)", Snackbar.LENGTH_LONG).show()
                                } else {
                                    uninstallApp(app)
                                }

                                "disable" -> if (app.packageName.contains("com.androidvip.hebf"))
                                    Snackbar.make(appsManagerCl, "Very funny :)", Snackbar.LENGTH_SHORT).show()
                                else
                                    disablePackage(app.packageName)

                                "enable" -> {
                                    runCommand("pm enable ${app.packageName}")
                                    Snackbar.make(appsManagerCl, "Package enabled", Snackbar.LENGTH_LONG).show()
                                    Logger.logInfo("Package enabled: ${app.packageName}", context)
                                    refreshList(currentFilter)
                                }
                            }
                        }
                    }
                    show(supportFragmentManager, "sheet")
                }
                true
            }
        }

        override fun getItemCount(): Int {
            return dataSet.size
        }

    }

    companion object {
        private var currentFilter = "-3"
    }
}