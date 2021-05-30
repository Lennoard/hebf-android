package com.androidvip.hebf.ui.main.battery.doze

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.R
import com.androidvip.hebf.utils.Doze

class DozeRecordAdapter(private val mContext: Context, private val dataSet: List<Doze.DozeRecord>) : RecyclerView.Adapter<DozeRecordAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var recordCount: TextView = v.findViewById(R.id.doze_record_count)
        var recordType: TextView = v.findViewById(R.id.doze_record_type)
        var recordDuration: TextView = v.findViewById(R.id.doze_record_duration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(mContext).inflate(R.layout.list_item_doze_record, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recordDuration = dataSet[position].state

        holder.recordType.typeface = Typeface.DEFAULT

        when (recordDuration) {
            "deep-idle" -> {
                holder.recordType.setText(R.string.doze_record_type_deep)
                holder.recordType.typeface = Typeface.DEFAULT_BOLD
            }
            "normal" -> holder.recordType.setText(R.string.doze_record_type_normal)
            "light-idle" -> holder.recordType.setText(R.string.doze_record_type_light)
            "light-maint" -> holder.recordType.setText(R.string.doze_record_type_light_maint)
            "deep-maint" -> holder.recordType.setText(R.string.doze_record_type_deep_maint)
            else -> holder.recordType.text = "??"
        }

        holder.recordDuration.text = "${dataSet[position].durationTime} ago ${dataSet[position].reason ?: ""}"
        holder.recordCount.text = (position + 1).toString()
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}