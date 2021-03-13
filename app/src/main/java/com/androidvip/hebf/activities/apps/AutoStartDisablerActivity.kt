package com.androidvip.hebf.activities.apps

import android.app.SearchManager
import android.content.Context
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.R
import com.androidvip.hebf.activities.BaseActivity
import com.androidvip.hebf.adapters.AutoStartAdapter
import com.androidvip.hebf.models.App
import com.androidvip.hebf.models.OpApp
import com.androidvip.hebf.toast
import com.androidvip.hebf.utils.*
import kotlinx.android.synthetic.main.activity_app_ops.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.Comparator

class AutoStartDisablerActivity : BaseActivity() {
    private lateinit var rv: RecyclerView
    private lateinit var adapter: AutoStartAdapter
    private val opApps: MutableList<OpApp> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_ops)

        setUpToolbar(toolbar)

        appOpsInfoCard.visibility = View.VISIBLE
        appOpsInfoCardButton.setOnClickListener { appOpsInfoCard.visibility = View.GONE }

        appOpsAppProgress.visibility = View.VISIBLE

        setupRecyclerView()

        Logger.logDebug("Retrieving BOOT_COMPLETED ops", this)
        lifecycleScope.launch(workerContext) {
            val opStatus = RootUtils
                .executeSync("appops query-op BOOT_COMPLETED allow")
                .toLowerCase(Locale.US)
            val supported = opStatus.isNotEmpty()
                && !opStatus.contains("unknown")
                && !opStatus.contains("error")

            if (supported) {
                val appsList = PackagesManager(this@AutoStartDisablerActivity).getThirdPartyApps()
                appsList.sortWith { one: App, other: App -> one.label.compareTo(other.label) }

                appsList.forEach { app ->
                    val opApp = OpApp("BOOT_COMPLETED", app).apply {
                        isOpEnabled = RootUtils.executeWithOutput(
                            "appops get ${this.packageName} ${this.op}", "allow",
                            this@AutoStartDisablerActivity
                        ).contains("allow")
                    }
                    opApps.add(opApp)
                }
            }

            runSafeOnUiThread {
                if (supported) {
                    adapter = AutoStartAdapter(this@AutoStartDisablerActivity, opApps)
                    rv.adapter = adapter
                    appOpsAppProgress.visibility = View.GONE
                } else {
                    toast("Not supported: Operation not found")
                    finish()
                }
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
        menu.findItem(R.id.action_filter).isVisible = false

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager?
        val searchView = searchMenuItem.actionView as SearchView
        searchView.queryHint = getString(R.string.search)
        if (searchManager != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
            searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String): Boolean {
                            adapter = AutoStartAdapter(this@AutoStartDisablerActivity, filterListByMatch(query))
                            rv.adapter = adapter
                            return true
                        }

                        override fun onQueryTextChange(newText: String): Boolean {
                            if (newText.length > 2) {
                                adapter = AutoStartAdapter(this@AutoStartDisablerActivity, filterListByMatch(newText))
                                rv.adapter = adapter
                                return true
                            } else if (newText.isEmpty()) {
                                adapter = AutoStartAdapter(this@AutoStartDisablerActivity, opApps)
                                rv.adapter = adapter
                                return true
                            }
                            return false
                        }
                    })
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    adapter = AutoStartAdapter(this@AutoStartDisablerActivity, opApps)
                    rv.adapter = adapter
                    return true
                }
            })
        }
        return true
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fragment_close_enter, android.R.anim.slide_out_right)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        rv = findViewById(R.id.app_ops_rv)

        val layoutManager = GridLayoutManager(applicationContext, defineRecyclerViewColumns())
        rv.setHasFixedSize(true)
        rv.layoutManager = layoutManager
    }

    private fun defineRecyclerViewColumns(): Int {
        val isTablet = resources.getBoolean(R.bool.is_tablet)
        val isLandscape = resources.getBoolean(R.bool.is_landscape)
        return if (isTablet || isLandscape) 2 else 1
    }

    private fun filterListByMatch(query: String): List<OpApp> {
        val filteredList = ArrayList<OpApp>()
        for (app in opApps)
            if (app.label.toLowerCase(Locale.getDefault()).contains(query.toLowerCase(Locale.getDefault())))
                filteredList.add(app)
        return filteredList
    }
}