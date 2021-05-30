package com.androidvip.hebf.ui.main.tune.cpu

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.R
import com.androidvip.hebf.utils.*
import java.util.*

class CpuTunableAdapter(private val activity: Activity, private val mDataSet: ArrayList<CpuManager.GovernorTunable>?) : RecyclerView.Adapter<CpuTunableAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var name: TextView = v.findViewById(R.id.paramNameLarge)
        var value: TextView = v.findViewById(R.id.cpu_tunable_value)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(activity).inflate(R.layout.list_item_generic_param_large, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (name, value, path) = mDataSet!![position]

        holder.name.text = name
        holder.value.text = value

        holder.itemView.setOnClickListener {
            if (value.isNullOrEmpty() || path.isNullOrEmpty() || value.contains("error"))
                Toast.makeText(activity, R.string.unsafe_operation, Toast.LENGTH_SHORT).show()
            else {
                EditDialog(activity).buildApplying {
                    title = name!!
                    inputText = value
                    inputHint = value
                    guessInputType = true
                    onConfirmListener = object : EditDialog.OnConfirmListener {
                        override fun onOkButtonClicked(newData: String) {
                            if (newData.isEmpty())
                                Utils.showEmptyInputFieldSnackbar(holder.name)
                            else {
                                RootUtils.executeAsync("echo '${newData.trim()}' > $path")
                                holder.value.text = newData.trim()

                                val achievementsSet = UserPrefs(activity).getStringSet(K.PREF.ACHIEVEMENT_SET, HashSet())
                                if (!achievementsSet.contains("tunable")) {
                                    Utils.addAchievement(activity.applicationContext, "tunable")
                                    Toast.makeText(activity, activity.getString(R.string.achievement_unlocked, activity.getString(R.string.achievement_tunable)), Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }.show()
            }
        }

    }

    override fun getItemCount(): Int {
        return mDataSet?.size ?: 0
    }
}
