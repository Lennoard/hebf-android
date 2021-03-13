package com.androidvip.hebf.fragments.doze

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.R
import com.androidvip.hebf.activities.BaseActivity
import com.androidvip.hebf.adapters.ForceStopAppsAdapter
import com.androidvip.hebf.fragments.BaseFragment
import com.androidvip.hebf.models.App
import com.androidvip.hebf.runSafeOnUiThread
import com.androidvip.hebf.toast
import com.androidvip.hebf.utils.Doze
import com.androidvip.hebf.utils.Logger
import com.androidvip.hebf.utils.PackagesManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_doze_whitelist.*
import kotlinx.coroutines.launch
import java.util.ArrayList
import kotlin.Comparator

@RequiresApi(api = Build.VERSION_CODES.M)
class DozeWhitelistFragment : BaseFragment() {
    private val packagesManager: PackagesManager by lazy { PackagesManager(findContext()) }
    private val whitelistedApps = mutableListOf<App>()

    private lateinit var adapter: RecyclerView.Adapter<*>
    private lateinit var fab: FloatingActionButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_doze_whitelist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(rvDozeWhitelist) {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(findContext(), defineRecyclerViewColumns())
            addItemDecoration(DividerItemDecoration(this.context, RecyclerView.VERTICAL))
            itemAnimator = null
        }

        val typedValueAccent = TypedValue()
        activity?.theme?.resolveAttribute(R.attr.colorAccent, typedValueAccent, true)
        dozeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(findContext(), typedValueAccent.resourceId))
        dozeRefreshLayout.setOnRefreshListener { getWhitelistedApps() }

        if (isActivityAlive) {
            fab = requireActivity().findViewById(R.id.doze_fab)
        }

        getWhitelistedApps()
    }

    private fun getWhitelistedApps() {
        dozeRefreshLayout.isRefreshing = true

        Logger.logDebug("Retrieving whitelisted apps", findContext())
        lifecycleScope.launch(workerContext) {
            whitelistedApps.clear()
            whitelistedApps.addAll(Doze.getWhitelistedApps(findContext()))
            whitelistedApps.sortWith { o1, o2 -> o1.label.compareTo(o2.label) }

            runSafeOnUiThread {
                dozeRefreshLayout.isRefreshing = false
                adapter = AppsAdapter(requireActivity() as BaseActivity, whitelistedApps)
                rvDozeWhitelist.adapter = adapter
            }
        }
    }

    override fun onResume() {
        super.onResume()

        runCatching {
            fab.show()
            fab.setOnClickListener { showPickAppsDialog() }
        }
    }

    override fun onPause() {
        super.onPause()

        runCatching {
            fab.hide()
        }
    }

    private fun showPickAppsDialog() {
        val snackbar = Snackbar.make(fab, R.string.loading, Snackbar.LENGTH_INDEFINITE)
        snackbar.show()

        val builder = MaterialAlertDialogBuilder(findContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_list_force_stop_apps, null)

        builder.setTitle(R.string.choose_package)
        builder.setView(dialogView)

        lifecycleScope.launch(workerContext) {
            val allPackages = packagesManager.installedPackages
            val allApps = ArrayList<App>()
            val whitelistedSet = whitelistedApps.map { it.packageName }

            allPackages.forEach {
                allApps.add(App().apply {
                    packageName = it
                    label = packagesManager.getAppLabel(it)
                    icon = packagesManager.getAppIcon(it)
                    isChecked = whitelistedSet.contains(it)
                })
            }

            allApps.sortWith(Comparator { one: App, other: App -> one.label.compareTo(other.label) })

            whitelistedApps.forEach {
                if (allApps.contains(it)) {
                    allApps.remove(it)
                }
            }

            runSafeOnUiThread {
                val rv = dialogView.findViewById<RecyclerView>(R.id.force_stop_apps_rv)
                val layoutManager = LinearLayoutManager(findContext(), RecyclerView.VERTICAL, false)
                rv.layoutManager = layoutManager

                val adapter = ForceStopAppsAdapter(requireActivity(), allApps)
                rv.adapter = adapter

                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    val appSet = adapter.selectedApps
                    val stringSet = appSet.map { it.packageName }

                    if (appSet.isNotEmpty()) {
                        Logger.logInfo("Doze whitelisting $stringSet", findContext())
                        launch(workerContext) {
                            Doze.whitelist(stringSet)

                            runSafeOnUiThread {
                                getWhitelistedApps()
                            }
                        }
                    }
                }
                builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
                builder.show()

                snackbar.dismiss()
            }
        }
    }

    private fun defineRecyclerViewColumns(): Int {
        return runCatching {
            if (isAdded) {
                val isTablet = resources.getBoolean(R.bool.is_tablet)
                val isLandscape = resources.getBoolean(R.bool.is_landscape)
                return if (isTablet || isLandscape) 2 else 1
            }
            return 1
        }.getOrDefault(1)
    }

    private class AppsAdapter(
        private val activity: BaseActivity,
        private val mDataSet: MutableList<App>
    ) : RecyclerView.Adapter<AppsAdapter.ViewHolder>() {

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            var appName: TextView = v.findViewById(R.id.app_name)
            var appPackageName: TextView = v.findViewById(R.id.app_package_name)
            var appType: TextView = v.findViewById(R.id.app_type)
            var appStatus: TextView = v.findViewById(R.id.app_status)
            var appIcon: ImageView = v.findViewById(R.id.app_icon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(activity).inflate(
                R.layout.list_item_apps_manager, parent, false
            )
            return ViewHolder(v)
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = mDataSet[position]
            val packageName = app.packageName

            holder.appType.text = "user"
            holder.appName.text = app.label
            val id = app.id.toString()
            if (id != "0") holder.appStatus.text = id

            holder.appPackageName.apply {
                setSingleLine(true)
                text = packageName
                ellipsize = TextUtils.TruncateAt.MARQUEE
                isSelected = true
                marqueeRepeatLimit = 5
            }

            holder.appIcon.setImageDrawable(app.icon)

            holder.itemView.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.transparent))

            if (app.isSystemApp) {
                holder.appType.text = "system"
            }

            holder.itemView.setOnClickListener {
                activity.toast("Long press to whitelist")
            }

            holder.itemView.setOnLongClickListener {
                MaterialAlertDialogBuilder(activity)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(R.string.doze_whitelist_remove_app)
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val appToRemove = mDataSet[holder.adapterPosition]
                        if (appToRemove.isDozeProtected)
                            Toast.makeText(
                                activity,
                                R.string.doze_blacklist_gms,
                                Toast.LENGTH_LONG
                            ).show()
                        else {
                            runCatching {
                                mDataSet.removeAt(holder.adapterPosition)
                                notifyDataSetChanged()
                            }

                            activity.toast(String.format(
                                activity.getString(R.string.doze_blacklist_app_added), appToRemove.label)
                            )
                            activity.lifecycleScope.launch(activity.workerContext) {
                                Doze.blacklist(appToRemove.packageName)
                            }
                        }
                    }
                    .show()
                true
            }
        }

        override fun getItemCount(): Int {
            return mDataSet.size
        }
    }
}
