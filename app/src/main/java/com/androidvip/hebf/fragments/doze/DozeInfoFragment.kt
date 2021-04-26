package com.androidvip.hebf.fragments.doze

import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.androidvip.hebf.R
import com.androidvip.hebf.adapters.DozeRecordAdapter
import com.androidvip.hebf.runSafeOnUiThread
import com.androidvip.hebf.ui.base.BaseFragment
import com.androidvip.hebf.utils.Doze
import com.androidvip.hebf.utils.Logger
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

@RequiresApi(api = Build.VERSION_CODES.M)
class DozeInfoFragment : BaseFragment() {
    private lateinit var fab: FloatingActionButton
    private lateinit var rv: RecyclerView
    private lateinit var mAdapter: RecyclerView.Adapter<*>
    private val dozeRecords = mutableListOf<Doze.DozeRecord>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_doze_summary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.doze_record_recycler_view)

        val refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.doze_swipe_to_refresh_summary)
        val typedValueAccent = TypedValue()
        requireActivity().theme.resolveAttribute(R.attr.colorAccent, typedValueAccent, true)
        refreshLayout.setColorSchemeColors(ContextCompat.getColor(findContext(), typedValueAccent.resourceId))
        refreshLayout.setOnRefreshListener {
            Logger.logDebug("Retrieving doze records", findContext())
            lifecycleScope.launch (workerContext) {
                dozeRecords.clear()
                dozeRecords.addAll(Doze.getDozeRecords().reversed())
                runSafeOnUiThread {
                    refreshLayout.isRefreshing = false
                    mAdapter.notifyDataSetChanged()
                }
            }
        }
        refreshLayout.isRefreshing = true

        if (isActivityAlive) {
            fab = requireActivity().findViewById(R.id.doze_fab)
        }

        Logger.logDebug("Retrieving doze records", findContext())
        lifecycleScope.launch (workerContext) {
            dozeRecords.clear()
            dozeRecords.addAll(Doze.getDozeRecords().reversed())

            runSafeOnUiThread {
                refreshLayout.isRefreshing = false
                mAdapter = DozeRecordAdapter(findContext(), dozeRecords)

                val layoutManager = GridLayoutManager(findContext(), defineRecyclerViewColumns())
                rv.layoutManager = layoutManager
                rv.adapter = mAdapter
            }
        }
    }

    override fun onResume() {
        super.onResume()

        runCatching {
            fab.hide()
            fab.setOnClickListener(null)
        }
    }

    private fun defineRecyclerViewColumns(): Int {
        val isTablet = resources.getBoolean(R.bool.is_tablet)
        val isLandscape = resources.getBoolean(R.bool.is_landscape)
        return if (isTablet || isLandscape) 2 else 1
    }
}