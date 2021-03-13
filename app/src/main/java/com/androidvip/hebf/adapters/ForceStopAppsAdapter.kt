package com.androidvip.hebf.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.R
import com.androidvip.hebf.models.App

class ForceStopAppsAdapter(
    private val activity: Activity,
    private val apps: List<App>
) : RecyclerView.Adapter<ForceStopAppsAdapter.ViewHolder>() {
    val selectedApps: MutableSet<App> = mutableSetOf()

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var label: TextView = v.findViewById(R.id.app_nome)
        var icon: ImageView = v.findViewById(R.id.icon)
        var checkBox: CheckBox = v.findViewById(R.id.force_stop_apps_check)

        init {
            setIsRecyclable(false)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(activity).inflate(R.layout.list_item_small_app, parent, false)
        return ViewHolder(v).apply {
            setIsRecyclable(false)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[holder.adapterPosition]

        fun selectionChange(isChecked: Boolean) {
            if (isChecked) {
                selectedApps.add(app)
            } else {
                selectedApps.remove(app)
            }
            app.isChecked = isChecked
        }

        holder.checkBox.setOnCheckedChangeListener(null)

        if (app.isChecked) {
            holder.checkBox.isChecked = true
            selectedApps.add(app)
        } else {
            holder.checkBox.isChecked = false
            selectedApps.remove(app)
        }

        holder.checkBox.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            selectionChange(isChecked)
        }

        holder.itemView.setOnClickListener {
            selectionChange(!holder.checkBox.isChecked)
        }

        holder.label.text = app.label
        holder.icon.setImageDrawable(app.icon)
    }

    override fun getItemCount(): Int {
        return apps.size
    }
}