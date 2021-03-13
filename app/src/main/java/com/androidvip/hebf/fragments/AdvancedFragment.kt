package com.androidvip.hebf.fragments

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.R
import com.androidvip.hebf.activities.advanced.*
import com.androidvip.hebf.activities.internal.CommandLineActivity
import com.androidvip.hebf.utils.EditDialog
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.RootUtils
import com.androidvip.hebf.utils.Utils
import kotlinx.coroutines.launch
import java.util.*

class AdvancedFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_advanced, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val buildProp = view.findViewById<View>(R.id.advanced_card_build_prop)
        buildProp.setOnClickListener(startAnAdvancedActivity(BuildPropActivity::class.java))

        val dns = view.findViewById<View>(R.id.advanced_card_dns)
        dns.setOnClickListener(startAnAdvancedActivity(ManualDnsActivity::class.java))

        val entropy = view.findViewById<View>(R.id.advanced_card_entropy)
        entropy.setOnClickListener(startAnAdvancedActivity(EntropyActivity::class.java))

        val art = view.findViewById<View>(R.id.advanced_card_art)
        art.setOnClickListener(startAnAdvancedActivity(ArtCompilerFilter::class.java))

        val sysctl = view.findViewById<View>(R.id.advanced_card_sysctl)
        sysctl.setOnClickListener {
            runCatching {
                if (isSysctlGuiInstalled()) {
                    findContext().packageManager.getLaunchIntentForPackage(
                        "com.androidvip.sysctlgui"
                    ).apply {
                        startActivity(this)
                    }
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://play.google.com/store/apps/details?id=com.androidvip.sysctlgui")
                        setPackage("com.android.vending")
                    })
                }
            }
        }

        val wm = view.findViewById<View>(R.id.advanced_card_wm)
        wm.setOnClickListener(startAnAdvancedActivity(WindowManagerActivity::class.java))

        val userType = userPrefs.getInt(K.PREF.USER_TYPE, 1)
        if (userType == 1) {
            sysctl.visibility = View.GONE
            art.visibility = View.GONE
            wm.visibility = View.GONE
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            art.visibility = View.GONE
            wm.visibility = View.GONE
        }

        if (Build.VERSION.SDK_INT >= 29) {
            art.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.advanced, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (!isActivityAlive) return false

        when (item.itemId) {
            R.id.action_run_as_root -> {
                EditDialog(requireActivity()).buildApplying {
                    title = "Run as root"
                    onConfirmListener = object : EditDialog.OnConfirmListener {
                        override fun onOkButtonClicked(newData: String) {
                            if (newData.isEmpty()) {
                                Toast.makeText(
                                    findContext(),
                                    "Something wrong is not right ${String(Character.toChars(0x1F914))}",
                                    Toast.LENGTH_LONG
                                ).show()
                                return
                            }

                            val achievementsSet = userPrefs.getStringSet(K.PREF.ACHIEVEMENT_SET, HashSet())
                            if (!achievementsSet.contains("command-line")) {
                                Utils.addAchievement(applicationContext, "command-line")
                                Toast.makeText(
                                    findContext(),
                                    getString(R.string.achievement_unlocked, getString(R.string.achievement_command_line)),
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            val resultDialog = Dialog(findContext()).apply {
                                setContentView(R.layout.dialog_log)
                                setTitle("Output")
                                setCancelable(true)
                            }

                            runCatching {
                                lifecycleScope.launch {
                                    val output = RootUtils.executeWithOutput(newData, "", activity)
                                    val holder = resultDialog.findViewById<TextView>(R.id.log_holder)
                                    holder.text = output
                                }

                            }

                            resultDialog.show()
                        }
                    }
                }.show()
                return true
            }

            R.id.action_hebf_cmd -> {
                startActivity(Intent(findContext(), CommandLineActivity::class.java))
                return true
            }
        }


        return false
    }

    private fun startAnAdvancedActivity(activityClass: Class<*>): View.OnClickListener {
        return View.OnClickListener {
            if (isActivityAlive) {
                findContext().startActivity(Intent(context, activityClass))
                requireActivity().overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out)
            }
        }
    }

    private fun isSysctlGuiInstalled(): Boolean {
        return try {
            findContext().packageManager.getPackageInfo("com.androidvip.sysctlgui", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

}