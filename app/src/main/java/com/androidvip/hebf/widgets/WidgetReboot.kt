package com.androidvip.hebf.widgets

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.R
import com.androidvip.hebf.activities.BaseActivity
import com.androidvip.hebf.getThemedVectorDrawable
import com.androidvip.hebf.utils.RootUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.launch

class WidgetReboot : BaseActivity() {
    internal var d: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dialogItems = arrayOf(
            getString(R.string.reboot),
            getString(R.string.recovery),
            getString(R.string.bootloader),
            getString(R.string.sysui),
            getString(R.string.power_off)
        )
        val warnings = arrayOf(
            getString(R.string.reiniciar_aviso),
            getString(R.string.recovery_aviso),
            getString(R.string.fastboot_aviso),
            getString(R.string.sysui_aviso),
            getString(R.string.shut_aviso)
        )
        val commands = arrayOf(
            "reboot",
            "reboot recovery",
            "reboot bootloader",
            "pkill com.android.systemui",
            "reboot -p"
        )

        lifecycleScope.launch(workerContext) {
            val isRooted = Shell.rootAccess()
            runSafeOnUiThread {
                if (isRooted) {
                    MaterialAlertDialogBuilder(this@WidgetReboot).apply {
                        setTitle(getString(R.string.rebooter))
                        setSingleChoiceItems(dialogItems, 0) { _, _ -> }
                        setPositiveButton(android.R.string.ok) { dialog, _ ->
                            val position = (dialog as AlertDialog).listView.checkedItemPosition
                            d = showConfirmDialog(warnings[position], commands[position]).show()
                            dialog.dismiss()
                        }
                        setOnDismissListener {
                            if (d == null) {
                                finish()
                            }
                        }
                        setNegativeButton(android.R.string.cancel) { _, _ -> finish() }
                        show()
                    }
                } else {
                    Toast.makeText(this@WidgetReboot, "Only for rooted users. Must be root to reboot!", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }.start()

    }

    private fun showConfirmDialog(msg: String, command: String): MaterialAlertDialogBuilder {
        return MaterialAlertDialogBuilder(this@WidgetReboot)
            .setTitle(getString(R.string.rebooter))
            .setMessage(msg)
            .setIcon(getThemedVectorDrawable(R.drawable.ic_warning))
            .setPositiveButton("OK") { _, _ ->
                if (Shell.rootAccess()) {
                    RootUtils.executeSync(command)
                } else {
                    Toast.makeText(this, "Only for rooted users!", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
            .setNegativeButton(getString(R.string.cancelar)) { dialogInterface, _ ->
                dialogInterface.dismiss()
                finish()
            }
    }

}