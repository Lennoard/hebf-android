package com.androidvip.hebf.ui.main.tune.buildprop

import android.app.SearchManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.R
import com.androidvip.hebf.ui.base.BaseActivity
import com.androidvip.hebf.utils.*
import kotlinx.android.synthetic.main.activity_build_prop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class BuildPropActivity : BaseActivity() {
    private lateinit var props: MutableList<Pair<String, String>>
    private var propAdapter: PropAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_build_prop)
        setUpToolbar(toolbar)

        val layoutManager = GridLayoutManager(applicationContext, defineRecyclerViewColumns())
        buildPropRecyclerView.layoutManager = layoutManager
        buildPropRecyclerView.setHasFixedSize(true)
        val decoration = DividerItemDecoration(buildPropRecyclerView.context, layoutManager.orientation)
        buildPropRecyclerView.addItemDecoration(decoration)

        buildPropRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    buildPropFab.hide()
                } else {
                    buildPropFab.show()
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        buildPropProgress.visibility = View.VISIBLE

        lifecycleScope.launch {
            mountSystem(true)
            props = getProps()
            showRecyclerView()
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            mountSystem(false)
        }
    }

    override fun onDestroy() {
        lifecycleScope.launch { mountSystem(false) }
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_search, menu)

        val searchMenuItem = menu.findItem(R.id.action_search)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager?
        val searchView = searchMenuItem.actionView as SearchView
        if (searchManager != null) {
            searchView.queryHint = getString(R.string.search)
            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
            searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String): Boolean {
                            propAdapter = PropAdapter(filterListByMatch(query), this@BuildPropActivity)
                            buildPropRecyclerView.adapter = propAdapter
                            return true
                        }

                        override fun onQueryTextChange(newText: String): Boolean {
                            if (newText.length > 2) {
                                propAdapter = PropAdapter(filterListByMatch(newText), this@BuildPropActivity)
                                buildPropRecyclerView.adapter = propAdapter
                                return true
                            } else if (newText.isEmpty()) {
                                propAdapter = PropAdapter(props, this@BuildPropActivity)
                                buildPropRecyclerView.adapter = propAdapter
                                return true
                            }
                            return false
                        }
                    })
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    propAdapter = PropAdapter(props, this@BuildPropActivity)
                    buildPropRecyclerView.adapter = propAdapter
                    return true
                }
            })
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fragment_close_enter, R.anim.slide_out_right)
    }

    private suspend fun mountSystem(rw: Boolean) = withContext(Dispatchers.Default) {
        val options = if (rw) "rw,remount" else "ro,remount"

        Logger.logDebug("Mounting /system, options: $options", this@BuildPropActivity)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            RootUtils.execute(arrayOf(
                    "mount -o $options /",
                    "mount -o $options /system",
                    "chmod 644 /system/build.prop"
            ))
        } else {
            RootUtils.execute(arrayOf(
                    "mount -o $options /system",
                    "chmod 644 /system/build.prop"
            ))
        }
    }

    private fun showRecyclerView() {
        propAdapter = PropAdapter(props, this)

        buildPropRecyclerView.adapter = propAdapter
        buildPropProgress.visibility = View.GONE
    }

    private suspend fun getProps(): MutableList<Pair<String, String>> = withContext(Dispatchers.IO) {
        val props = mutableListOf<Pair<String, String>>()

        RootUtils.executeWithOutput(
                "cat /system/build.prop", "", this@BuildPropActivity
        ) { line ->
            if (line.isNotEmpty() && !line.startsWith("#")) {
                runCatching {
                    val split = line.split("=").dropLastWhile {
                        it.isEmpty()
                    }.toTypedArray()
                    props.add(split.first() to split[1])
                }
            }
        }
        return@withContext props
    }

    private fun filterListByMatch(query: String): MutableList<Pair<String, String>> {
        return props.filter { prop ->
            val key = prop.first.toLowerCase(Locale.US)
            val value = prop.second.toLowerCase(Locale.US)
            key.contains(query.toLowerCase(Locale.US)) || value.contains(query.toLowerCase(Locale.US))
        }.toMutableList()
    }

    private fun defineRecyclerViewColumns(): Int {
        val isTablet = resources.getBoolean(R.bool.is_tablet)
        val isLandscape = resources.getBoolean(R.bool.is_landscape)
        return if (isTablet || isLandscape) 2 else 1
    }
}
