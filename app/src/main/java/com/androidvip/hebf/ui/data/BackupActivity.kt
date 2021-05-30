package com.androidvip.hebf.ui.data

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.androidvip.hebf.R
import com.androidvip.hebf.applyAnim
import com.androidvip.hebf.ui.base.BaseActivity
import com.androidvip.hebf.utils.BackupUtils
import com.androidvip.hebf.utils.Logger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_backup.*

class BackupActivity : BaseActivity() {
    private val backupUtils: BackupUtils by lazy { BackupUtils(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)

        setUpToolbar(toolbar)

        backupInputModel.setText(Build.MODEL)
        backupInputDeviceName.setText(Build.DEVICE)

        backupUtils.setOnCompleteListener { taskType, isSuccessful ->
            backupProgress.visibility = View.GONE
            when (taskType) {
                BackupUtils.TASK_BACKUP -> {
                    if (isSuccessful) {
                        Snackbar.make(backupFab, R.string.backup_created, Snackbar.LENGTH_LONG)
                            .show()
                        Logger.logInfo("Backup created", this)
                    } else {
                        Snackbar.make(backupFab, R.string.backup_failure, Snackbar.LENGTH_LONG)
                            .show()
                        Logger.logError("Failed to crate backup", this)
                    }
                }
                BackupUtils.TASK_RESTORE -> {
                    if (isSuccessful) {
                        Snackbar.make(backupFab, R.string.data_restored, Snackbar.LENGTH_LONG)
                            .show()
                        Logger.logInfo("Backup restaured", this)
                    } else {
                        Snackbar.make(backupFab, R.string.data_restore_failed, Snackbar.LENGTH_LONG)
                            .show()
                        Logger.logError("Failed to restore backup", this)
                    }
                }
            }
        }

        backupRadioLocal.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                backupFab.hide()
                backupFab.setImageResource(R.drawable.ic_backup_local)
                backupFab.show()
                backupCheckPublic.visibility = View.GONE
                findViewById<View>(R.id.backup_comments_layout).visibility = View.GONE
                backupCheckPublic.isChecked = false
            }
        }

        backupCheckPublic.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
                findViewById<View>(R.id.backup_comments_layout).visibility = View.VISIBLE
            else
                findViewById<View>(R.id.backup_comments_layout).visibility = View.GONE
        }

        backupRadioCloud.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                backupRadioCloud.isChecked = false
                backupRadioLocal.isChecked = true
            }
        }

        backupFab.setOnClickListener {
            backupProgress.visibility = View.VISIBLE
            val filename = backupInputName.text.toString().trim()
            val deviceModel = backupInputModel.text.toString().trim()
            val deviceName = backupInputDeviceName.text.toString().trim()
            val comments = backupInputComments.text.toString()
            if (backupRadioLocal.isChecked) {
                try {
                    backupUtils.backupToFile(filename, deviceModel, deviceName)
                    Snackbar.make(backupFab, R.string.backup_created, Snackbar.LENGTH_SHORT).show()
                    backupProgress.visibility = View.GONE
                } catch (e: Exception) {
                    backupProgress.visibility = View.GONE
                    Snackbar.make(backupFab, R.string.backup_failure, Snackbar.LENGTH_LONG).show()
                    showErrorDialog(e.message)
                }
            } else {
                throw NotImplementedError()
            }
        }

        backupRadioCloud.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                backupFab.hide()
                backupFab.setImageResource(R.drawable.ic_backup)
                backupFab.show()
                backupCheckPublic.visibility = View.VISIBLE
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showErrorDialog(message: String?) {
        if (isFinishing) return

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.failed)
            .setMessage(message ?: "Failed to complete one of the operations")
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ -> backupProgress.visibility = View.GONE }
            .applyAnim()
            .show()
    }
}
