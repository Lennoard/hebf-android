package com.androidvip.hebf.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.R
import com.androidvip.hebf.adapters.CpuTunableAdapter
import com.androidvip.hebf.ui.base.BaseActivity
import com.androidvip.hebf.utils.CpuManager
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.Logger
import kotlinx.android.synthetic.main.activity_cpu_tuner.*
import java.util.*

class CpuTunerActivity : BaseActivity() {
    internal lateinit var rv: RecyclerView
    private lateinit var mAdapter: RecyclerView.Adapter<*>
    private var governorTunables: ArrayList<CpuManager.GovernorTunable>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_cpu_tuner)
        setUpToolbar(toolbar)

        try {
            governorTunables = intent.getSerializableExtra(K.EXTRA_GOVERNOR_TUNABLES) as ArrayList<CpuManager.GovernorTunable>?
            val governor = intent.getStringExtra("governor")
            if (governorTunables != null && !governor.isNullOrEmpty()) {
                supportActionBar!!.subtitle = governor
                setupRecyclerView()
            } else {
                Toast.makeText(this, "Could not read CPU governor parameters", Toast.LENGTH_SHORT).show()
                Logger.logWTF("Could not read CpuManager because no governor was provided to begin with", this)
                finish()
            }
        } catch (e: Exception) {
            Logger.logError("Could not read CpuManager governor parameters: ${e.message}", this)
            Toast.makeText(this, "Could not read CpuManager governor parameters", Toast.LENGTH_SHORT).show()
            finish()
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fragment_close_enter, android.R.anim.slide_out_right)
    }

    private fun setupRecyclerView() {
        val pb = findViewById<ProgressBar>(R.id.progress_cpu_tuner)
        pb.visibility = View.GONE

        rv = findViewById(R.id.rv_cpu_tunables)
        mAdapter = CpuTunableAdapter(this, governorTunables)

        val layoutManager = GridLayoutManager(this, recyclerViewColumns)
        val decoration = DividerItemDecoration(rv.context, LinearLayout.VERTICAL)
        rv.addItemDecoration(decoration)
        rv.setHasFixedSize(true)
        rv.layoutManager = layoutManager
        rv.adapter = mAdapter
    }

    private val recyclerViewColumns: Int
        get() {
            val isLandscape = resources.getBoolean(R.bool.is_landscape)
            return if (isLandscape) 2 else 1
        }
}
