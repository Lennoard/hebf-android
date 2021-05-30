package com.androidvip.hebf.ui.internal

import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.BuildConfig
import com.androidvip.hebf.R
import com.androidvip.hebf.applyAnim
import com.androidvip.hebf.ui.base.BaseActivity
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.Logger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_log.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// TODO: review target SDK 29
class LogActivity : BaseActivity() {
    private val logFile: File by lazy { K.HEBF.getLogFile(this) }
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        setUpToolbar(toolbar)

        refresh()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.log, menu)
        val searchMenuItem = menu.findItem(R.id.action_search)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager?
        val searchView = searchMenuItem.actionView as SearchView
        searchView.queryHint = getString(R.string.search)
        if (searchManager != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
            searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String): Boolean {
                            adapter.filter.filter(query)
                            return true
                        }

                        override fun onQueryTextChange(newText: String): Boolean {
                            if (newText.length > 4) {
                                adapter.filter.filter(newText)
                                return true
                            } else if (newText.isEmpty()) {
                                adapter.filter.filter("")
                                return true
                            }
                            return false
                        }
                    })
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    adapter.filter.filter("")
                    return true
                }
            })
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete -> MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.warning))
                    .setMessage(getString(R.string.confirmation_message))
                    .setNegativeButton(R.string.cancelar) { _, _ -> }
                    .setPositiveButton("OK") { _, _ ->
                        if (logFile.exists()) {
                            if (!logFile.delete()) {
                                Logger.logError("Could not delete log", this)
                            } else {
                                createLogFile()
                            }
                        }
                        refresh()
                    }
                    .applyAnim().also {
                        if (isAlive) {
                            it.show()
                        }
                    }
            R.id.action_send -> {
                val length = logFile.length()
                val logSize = if (length < 1024) "Log size: $length bytes" else "Log size: ${length / 1024}KB"

                Toast.makeText(this, logSize, Toast.LENGTH_SHORT).show()

                try {
                    val path = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", logFile)
                    val emailIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "vnd.android.cursor.dir/email"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("support@androidvip.com.br"))
                        putExtra(Intent.EXTRA_STREAM, path)
                        putExtra(Intent.EXTRA_SUBJECT, "HEBF Logs")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(emailIntent, "Send email..."))
                } catch (e: Exception) {
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                    Logger.logWTF(e.message, this)
                }

            }
            R.id.action_filter -> {
                val builder = MaterialAlertDialogBuilder(this)
                val array = arrayOf<CharSequence>(getString(R.string.none), "Info", getString(android.R.string.dialog_alert_title), getString(R.string.error), "Fatal", "What the…")
                builder.setTitle(getString(R.string.choose_one_filter))
                        .setSingleChoiceItems(array, 0) { _, _ -> }
                        .setPositiveButton(android.R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                            when ((dialog as AlertDialog).listView.checkedItemPosition) {
                                0 -> adapter.filter.filter("")
                                1 -> adapter.filter.filter("[INFO]")
                                2 -> adapter.filter.filter("[WARN]")
                                3 -> adapter.filter.filter("[ERROR]")
                                4 -> adapter.filter.filter("[FATAL]")
                                5 -> adapter.filter.filter("[WTF]")
                            }
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ -> }
                builder.show()
            }

            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun refresh() {
        lifecycleScope.launch {
            val logs = readLogs()
            adapter = LogAdapter(this@LogActivity, R.layout.list_item_small, logs)
            logsListView.adapter = adapter
        }
    }

    private suspend fun readLogs() = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            logFile.readLines()
        }.getOrDefault(mutableListOf("[ERROR] Failed to read log file"))
    }

    private fun createLogFile() {
        try {
            if (logFile.exists()) {
                if (logFile.createNewFile()) {
                    Logger.logError("Could not create log file", this)
                }
            }
        } catch (e: Exception) {
            Logger.logError("Error while saving the log file: ${e.message}", this)
        }
    }

    class LogAdapter(context: Context, resource: Int, items: List<String>) : ArrayAdapter<String>(context, resource, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)

            val textView = view.findViewById<TextView>(R.id.listItemText)
            val previousText = textView.text.toString()
            textView.text = previousText.toSpannable()

            textView.setOnLongClickListener {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val content = ClipData.newPlainText("log", textView.text)
                clipboard.setPrimaryClip(content)
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                true
            }

            return view
        }

        private fun findSubstringToSpan(sample: String): String {
            if (sample.isEmpty()) return sample

            val startIndex = sample.indexOf("[")
            val endIndex = sample.indexOf("]")

            if (startIndex < 0 || endIndex < 0) return sample

            return sample.substring(startIndex, endIndex + 1)
        }

        private fun String.toSpannable(): SpannableString {
            val substringToSpan = findSubstringToSpan(this)
            val spannable = SpannableString(this)

            when (substringToSpan) {
                "[DEBUG]" -> spannable.setSpan(ForegroundColorSpan(Color.parseColor("#8d6e63")), 0, substringToSpan.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                "[INFO]" -> spannable.setSpan(ForegroundColorSpan(Color.parseColor("#00BFA5")), 0, substringToSpan.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                "[WARN]" -> spannable.setSpan(ForegroundColorSpan(Color.parseColor("#FF9100")), 0, substringToSpan.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                "[ERROR]" -> spannable.setSpan(ForegroundColorSpan(Color.parseColor("#FF5252")), 0, substringToSpan.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                "[FATAL]" -> {
                    spannable.setSpan(ForegroundColorSpan(Color.parseColor("#D50000")), 0, substringToSpan.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannable.setSpan(StyleSpan(Typeface.BOLD), 0, substringToSpan.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                "[WTF]" -> spannable.setSpan(ForegroundColorSpan(Color.parseColor("#7C4DFF")), 0, substringToSpan.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            return spannable
        }
    }
}
