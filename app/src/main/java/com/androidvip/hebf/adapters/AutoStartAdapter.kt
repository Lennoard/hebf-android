package com.androidvip.hebf.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.R
import com.androidvip.hebf.activities.BaseActivity
import com.androidvip.hebf.models.OpApp

class AutoStartAdapter(
    private val activity: BaseActivity,
    private val dataSet: List<OpApp>
) : RecyclerView.Adapter<AutoStartAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var name: TextView
        var packageName: TextView
        var icon: ImageView
        var state: SwitchCompat
        var itemLayout: FrameLayout

        init {
            setIsRecyclable(false)
            name = v.findViewById(R.id.app_ops_app_name)
            packageName = v.findViewById(R.id.app_ops_app_package_name)
            icon = v.findViewById(R.id.app_ops_app_icon)
            state = v.findViewById(R.id.app_ops_app_switch)
            itemLayout = v.findViewById(R.id.app_ops_layout)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(activity).inflate(R.layout.list_item_app_ops_app_switch, parent, false)
        return ViewHolder(v).apply {
            setIsRecyclable(false)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val opApp = dataSet[position]

        holder.name.text = opApp.label
        holder.packageName.text = opApp.packageName
        holder.icon.setImageDrawable(opApp.icon)

        holder.state.setOnCheckedChangeListener(null)
        holder.state.isChecked = opApp.isOpEnabled
        holder.state.setOnCheckedChangeListener { _, isChecked -> setAppOp(opApp, isChecked) }

        if (opApp.packageName.contains("com.androidvip.hebf")) {
            holder.state.isEnabled = false
            holder.state.isChecked = true
        }

        holder.itemLayout.setOnClickListener {
            holder.state.isChecked = !holder.state.isChecked
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    private fun setAppOp(app: OpApp, enable: Boolean) {
        app.isOpEnabled = enable
        val state = if (enable) "allow" else "deny"
        activity.runCommand("appops set ${app.packageName} ${app.op} $state")
    }
}
