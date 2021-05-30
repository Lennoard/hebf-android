package com.androidvip.hebf.ui.main.tools.cleaner

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.R
import com.androidvip.hebf.ui.base.BaseActivity
import com.androidvip.hebf.models.Directory
import com.androidvip.hebf.roundDecimals
import com.androidvip.hebf.utils.RootUtils
import com.androidvip.hebf.utils.Utils
import com.google.android.material.checkbox.MaterialCheckBox
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

// TODO: Legacy
class DirectoryCleanerAdapter(
    private val activity: BaseActivity,
    private val dataSet: List<Directory>
) : RecyclerView.Adapter<DirectoryCleanerAdapter.ViewHolder>() {
    private var bytesToFree: Long = 0

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var dirName: TextView = v.findViewById(R.id.directory_cleaner_name)
        var dirPath: TextView = v.findViewById(R.id.directory_cleaner_path)
        var dirSize: TextView = v.findViewById(R.id.directory_cleaner_size)
        var dirDeleteCheck: MaterialCheckBox = v.findViewById(R.id.directory_cleaner_check)
        var dirLayout: LinearLayout = v.findViewById(R.id.directory_cleaner_layout)

        init {
            setIsRecyclable(false)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(activity).inflate(R.layout.list_item_directory_cleaner, parent, false)
        return ViewHolder(v).apply {
            setIsRecyclable(false)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val directory = dataSet[position]

        holder.dirName.text = directory.name
        holder.dirSize.text = normalizeSize(runCatching {
            directory.size / 1024
        }.getOrDefault(0))

        if (directory.path == "${Environment.getExternalStorageDirectory()}/Android/data")
            holder.dirPath.text = "${Environment.getExternalStorageDirectory()}/Android/data/*/cache"
        else {
            if (directory.path == "${Environment.getDataDirectory()}/data")
                holder.dirPath.text = "${Environment.getDataDirectory()}/data/*/cache"
            else
                holder.dirPath.text = directory.path
        }

        holder.dirDeleteCheck.setOnCheckedChangeListener { _, isChecked ->
            val previousBytes = bytesToFree + 0L
            val storageDetails = activity.findViewById<AppCompatTextView>(R.id.cleaner_storage_details)

            activity.lifecycleScope.launch (activity.workerContext) {
                if (isChecked) {
                    // Increase bytesToFree by this directory size
                    bytesToFree += directory.size
                    // We must correct these paths in order to delete only [?]/data/*/cache dirs
                    if (directory.path == "${Environment.getExternalStorageDirectory()}/Android/data") {
                        // This list will hold .../Android/data/com.package.name/cache dirs
                        pathsToDelete.addAll(listCacheSubDirs(directory.file))
                    } else {
                        if (directory.path == "${Environment.getDataDirectory()}/data") {
                            // This list will hold /data/data/com.package.name/cache dirs
                            pathsToDelete.addAll(listCacheSubDirs(directory.file))
                        } else {
                            pathsToDelete.add(directory.path)
                        }
                    }
                } else {
                    // Decrease bytesToFree by this directory size
                    bytesToFree -= directory.size
                    if (directory.path == "${Environment.getExternalStorageDirectory()}/Android/data") {
                        pathsToDelete.removeAll(listCacheSubDirs(directory.file))
                    } else
                        pathsToDelete.remove(directory.path)
                }

                activity.runSafeOnUiThread {
                    val kb = bytesToFree / 1024
                    val previousKb = previousBytes / 1044

                    ValueAnimator.ofInt(previousKb.toInt(), kb.toInt()).apply {
                        duration = 250
                        interpolator = AccelerateInterpolator()
                        addUpdateListener {
                            storageDetails.text = normalizeSize((it.animatedValue as Number).toLong())
                        }
                        start()
                    }

                    storageDetails.tag = normalizeSize(bytesToFree)
                }
            }
        }

        if (directory.isChecked) {
            holder.dirDeleteCheck.isChecked = true
        }

        holder.dirLayout.setOnClickListener {
            if (holder.dirDeleteCheck.isChecked) {
                holder.dirDeleteCheck.isChecked = false
                directory.isChecked = false
            } else {
                holder.dirDeleteCheck.isChecked = true
                directory.isChecked = true
            }
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    private fun normalizeSize(kb: Long): String {
        if (kb >= 1048576) {
            val megaBytes = kb.toDouble() / 1024.0
            return "${(megaBytes / 1024.0).roundDecimals(2)} GB"
        }
        return if (kb >= 1024) {
            "${(kb / 1024.0).roundDecimals(2)} MB"
        } else {
            "${(kb.toDouble()).roundDecimals(1)} KB"
        }
    }

    private suspend fun listCacheSubDirs(path: File): List<String> {
        if (path.canRead()) {
            val subDirs = path.list { current, name -> File(current, name).isDirectory }

            subDirs?.indices?.forEach { i ->
                subDirs[i] = "$path/${subDirs[i]}/cache"
            }
            return listOf(*subDirs ?: arrayOf())
        }

        val subDirs = mutableListOf<String>()
        val command = if (Utils.runCommand("which busybox", "").isNotEmpty()) {
            "busybox ls -1 $path"
        } else {
            "ls -1 $path"
        }

        RootUtils.executeWithOutput(command, "", null) { line ->
            if (line.isNotEmpty() && line.trim() != "android") {
                subDirs.add("$path/${line.trim()}/cache")
            }
        }

        return subDirs
    }

    companion object {
        private val pathsToDelete = ArrayList<String>()

        @Synchronized
        fun getPathsToDelete(): List<String> {
            return pathsToDelete
        }
    }
}