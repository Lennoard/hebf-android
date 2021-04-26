package com.androidvip.hebf.activities.apps

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.R
import com.androidvip.hebf.getThemedVectorDrawable
import com.androidvip.hebf.models.App
import com.androidvip.hebf.ui.base.BaseActivity
import com.androidvip.hebf.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_app_details.*
import kotlinx.coroutines.launch
import java.io.File

// Todo: check target sdk 29
class AppDetailsActivity : BaseActivity() {
    private lateinit var appPackageName: TextView
    private lateinit var appVersion: TextView
    private lateinit var storageDetails: TextView
    private lateinit var pathDetails: TextView
    private lateinit var storage: LinearLayout
    private lateinit var path: LinearLayout
    private lateinit var appOps: FrameLayout
    private lateinit var appIcon: ImageView
    private lateinit var packagesManager: PackagesManager

    private var sdSize: Long = 0
    private var internalSize: Long = 0
    private var appSize: Long = 0
    private lateinit var apkFile: File
    private var aPackageName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_details)
        setUpToolbar(toolbar)

        bindViews()

        packagesManager = PackagesManager(this)

        // Check the data from intent
        val app = intent.getSerializableExtra(K.EXTRA_APP) as App?
        if (app != null) {
            aPackageName = app.packageName

            // Update Toolbar title with app name
            supportActionBar?.title = app.label
            // Show info according to the given package name
            appIcon.setImageDrawable(packagesManager.getAppIcon(app.packageName))
            appPackageName.text = app.packageName
            appVersion.text = "v${app.versionName}"
            // Get disabled state of the package and set button text accordingly
            if (!app.isEnabled) appDetailsDisable.setText(R.string.enable)

            appOps.setOnClickListener {
                val i = Intent(this@AppDetailsActivity, AppOpsActivity::class.java)
                i.putExtra(K.EXTRA_APP, intent.getSerializableExtra(K.EXTRA_APP))
                startActivity(i)
            }

            lifecycleScope.launch(workerContext) {
                // Check if the system has the appops binary
                val supportsAppOps = RootUtils.executeSync("which appops").isNotEmpty()

                // Get the package installation path from the PackageManager
                val pathString = Utils.runCommand(
                    "pm path ${aPackageName!!}",
                    getString(android.R.string.unknownName)
                ).replace("package:", "")

                apkFile = File(pathString)
                if (apkFile.exists()) {
                    appSize = runCatching {
                        // Get root installation folder size
                        RootUtils.executeWithOutput(
                            "du -s ${apkFile.parentFile.absolutePath} | awk '{print $1}'",
                            "0"
                        ).toLong()
                    }.getOrElse {
                        runCatching {
                            RootUtils.executeWithOutput(
                                "du -s ${apkFile.parentFile.absolutePath} | awk '{print $1}'",
                                "0"
                            ).toLong()
                        }.getOrDefault(0)
                    }
                }

                try {
                    // Get external folder size
                    sdSize = FileUtils.getFileSize(File("${Environment.getExternalStorageDirectory()}/Android/data/$aPackageName")) / 1024
                    // Get root data folder size
                    internalSize = RootUtils.executeWithOutput("du -s /data/data/$aPackageName | awk '{print $1}'", "0").toLong()
                } catch (e: Exception) {
                    try { //again
                        sdSize = FileUtils.getFileSize(File("${Environment.getExternalStorageDirectory()}/Android/data/$aPackageName")) / 1024
                        internalSize = RootUtils.executeWithOutput("du -s /data/data/$aPackageName | awk '{print $1}'", "0").toLong()
                    } catch (ex: Exception) {
                        sdSize = 0
                        internalSize = 0
                    }
                }

                runSafeOnUiThread {
                    /*
                     * Compute and show total storage size on the UI Thread.
                     * Set pathDetails text to the package path obtained from PackageManager,
                     * this path is used to share .apk file of the app.
                     */
                    val finalSize = (sdSize + internalSize + appSize) / 1024
                    pathDetails.text = pathString
                    storageDetails.text = "$finalSize MB"

                    findViewById<View>(R.id.app_details_progress).visibility = View.GONE
                    findViewById<View>(R.id.app_details_detail_layout).visibility = View.VISIBLE

                    if (!supportsAppOps) {
                        appOps.visibility = View.GONE
                        Logger.logWarning("Appops is not supported", this@AppDetailsActivity)
                    }
                }
            }

            // Show storage usage details
            storage.setOnClickListener {
                MaterialAlertDialogBuilder(this@AppDetailsActivity)
                    .setTitle(R.string.storage)
                    .setMessage("SD: ${if (sdSize >= 1024) "${(sdSize / 1024)}MB\n" else "${sdSize}KB\n"}Internal: ${if (internalSize >= 1024) "${(internalSize / 1024)}MB\n" else "${internalSize}KB\n"}App: ${appSize / 1024}MB")
                    .setNeutralButton(R.string.clear_data) { _, _ ->
                        // Clear data associated with the package using the PackageManager
                        runCommand("pm clear ${aPackageName!!}")
                        Logger.logInfo("Cleared data of the package: ${aPackageName!!}", this)
                    }
                    .setNegativeButton(R.string.close) { _, _ -> }
                    .show()
            }

            path.setOnClickListener {
                if (apkFile.isFile) {
                    // Share .apk file
                    val uri = Uri.parse(apkFile.toString())
                    val share = Intent(Intent.ACTION_SEND)
                    share.type = "application/octet-stream"
                    share.putExtra(Intent.EXTRA_STREAM, uri)
                    startActivity(Intent.createChooser(share, "Share APK File"))
                }
            }

            // Disable or enable package
            appDetailsDisable.setOnClickListener {
                if (app.isEnabled) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.warning))
                        .setIcon(getThemedVectorDrawable(R.drawable.ic_warning))
                        .setMessage(getString(R.string.confirmation_message))
                        .setNegativeButton(R.string.cancelar) { _, _ -> }
                        .setPositiveButton(R.string.disable) { _, _ ->
                            runCommand("pm disable ${aPackageName!!}")
                            Logger.logInfo("Disabled package: ${aPackageName!!}", this)
                            Snackbar.make(appDetailsDisable, R.string.package_disabled, Snackbar.LENGTH_LONG).show()
                            // Update button text
                            appDetailsDisable.setText(R.string.enable)
                        }
                        .show()
                } else {
                    // Enable package using the PackageManager and update button text
                    runCommand("pm enable ${aPackageName!!}")
                    Logger.logInfo("Enabled package: ${aPackageName!!}", this)
                    appDetailsDisable.setText(R.string.disable)
                }
            }

            appDetailsUninstall.setOnClickListener {
                Logger.logWarning("Attempting to uninstall package: ${aPackageName!!}", this)
                if (app.isSystemApp) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.warning))
                        .setIcon(getThemedVectorDrawable(R.drawable.ic_warning))
                        .setMessage(getString(R.string.confirmation_message))
                        .setNegativeButton(android.R.string.cancel) { _, _ -> }
                        .setPositiveButton(R.string.uninstall) { _, _ ->
                            lifecycleScope.launch(workerContext) {
                                // Get package path
                                val packagePathString = RootUtils.executeWithOutput("pm path ${aPackageName!!}", "", this@AppDetailsActivity).substring(8)
                                val packagePath = File(packagePathString)
                                if (packagePath.isFile) {
                                    RootUtils.deleteFileOrDir(packagePathString)
                                    RootUtils.deleteFileOrDir("${Environment.getDataDirectory()}/data/$aPackageName")

                                    Logger.logInfo("Deleted package: ${aPackageName!!}", this@AppDetailsActivity)
                                    runSafeOnUiThread {
                                        MaterialAlertDialogBuilder(this@AppDetailsActivity)
                                            .setTitle(R.string.package_uninstalled)
                                            .setMessage("Reboot your device")
                                            .setPositiveButton(android.R.string.ok) { _, _ -> }
                                            .setNeutralButton(R.string.reboot) { _, _ -> runCommand("reboot") }
                                            .show()
                                    }
                                } else {
                                    runSafeOnUiThread {
                                        Snackbar.make(appDetailsUninstall, "${getString(R.string.error)}: $packagePath does not exist", Snackbar.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                        .show()
                } else {
                    // User app, invoke Android uninstall dialog and let it deal with the package
                    try {
                        val packageURI = Uri.parse("package:${aPackageName!!}")
                        val uninstallIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI)
                        startActivity(uninstallIntent)
                    } catch (e: Exception) {
                        Toast.makeText(this@AppDetailsActivity, "Could not launch uninstall dialog for package: $aPackageName. Reason: ${e.message}", Toast.LENGTH_LONG).show()
                        Logger.logError("Could not launch uninstall dialog for package: $aPackageName. Reason: ${e.message}", this@AppDetailsActivity)
                    }
                }
            }

        } else {
            // Something went wrong, return back to the previous screen
            Logger.logWTF("Failed to show app details because no app was provided to begin with", this)
            Toast.makeText(this, R.string.failed, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun bindViews() {
        appPackageName = findViewById(R.id.app_details_package_name)
        appVersion = findViewById(R.id.app_details_version)
        appIcon = findViewById(R.id.app_details_icon)
        appOps = findViewById(R.id.app_details_app_ops)

        storage = findViewById(R.id.app_details_storage)
        storageDetails = findViewById(R.id.app_details_storage_sum)
        path = findViewById(R.id.app_details_path)
        pathDetails = findViewById(R.id.app_details_path_sum)
    }
}
