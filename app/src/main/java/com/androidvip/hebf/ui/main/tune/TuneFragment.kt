package com.androidvip.hebf.ui.main.tune

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.androidvip.hebf.*
import com.androidvip.hebf.databinding.FragmentTuneBinding
import com.androidvip.hebf.ui.base.binding.BaseViewBindingFragment
import com.androidvip.hebf.ui.internal.CommandLineActivity
import com.androidvip.hebf.ui.main.LottieAnimViewModel
import com.androidvip.hebf.ui.main.tune.buildprop.BuildPropActivity
import com.androidvip.hebf.ui.main.tune.cpu.CpuManagerActivity
import com.androidvip.hebf.utils.EditDialog
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.RootUtils
import com.androidvip.hebf.utils.Utils
import com.google.android.material.behavior.SwipeDismissBehavior
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.*

class TuneFragment : BaseViewBindingFragment<FragmentTuneBinding>(
    FragmentTuneBinding::inflate
) {
    private val animViewModel: LottieAnimViewModel by sharedViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cpuManager.setOnClickListener(startAnAdvancedActivity(CpuManagerActivity::class.java))
        binding.buildProp.setOnClickListener(startAnAdvancedActivity(BuildPropActivity::class.java))
        binding.dns.setOnClickListener(startAnAdvancedActivity(ManualDnsActivity::class.java))
        binding.entropy.setOnClickListener(startAnAdvancedActivity(EntropyActivity::class.java))
        binding.wm.setOnClickListener(startAnAdvancedActivity(WindowManagerActivity::class.java))
        binding.hebfTerminal.setOnClickListener(startAnAdvancedActivity(CommandLineActivity::class.java))

        binding.ramManager.setOnClickListener {
            findNavController().navigate(R.id.startRamManagerFragment)
        }

        binding.sysctl.setOnClickListener {
            runCatching {
                if (isSysctlGuiInstalled()) {
                    findContext().packageManager.getLaunchIntentForPackage(
                        "com.androidvip.sysctlgui"
                    ).apply {
                        startActivity(this)
                    }
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW).apply {
                        data =
                            Uri.parse("https://play.google.com/store/apps/details?id=com.androidvip.sysctlgui")
                        setPackage("com.android.vending")
                    })
                }
            }
        }

        binding.art.setOnClickListener(
            startAnAdvancedActivity(ArtCompilerFilter::class.java)
        )

        binding.runAsRoot.setOnClickListener {
            EditDialog(requireActivity()).buildApplying {
                title = "Run as root"
                onConfirmListener = object : EditDialog.OnConfirmListener {
                    override fun onOkButtonClicked(newData: String) {
                        if (newData.isEmpty()) {
                            val emoji = String(Character.toChars(0x1F914))
                            requireContext().toast("Something wrong is not right $emoji")
                            return
                        }

                        val achievementsSet =
                            userPrefs.getStringSet(K.PREF.ACHIEVEMENT_SET, HashSet())
                        if (!achievementsSet.contains("command-line")) {
                            Utils.addAchievement(applicationContext, "command-line")
                            requireContext().toast(
                                getString(
                                    R.string.achievement_unlocked,
                                    getString(R.string.achievement_command_line)
                                )
                            )
                        }

                        val resultDialog = Dialog(findContext()).apply {
                            setContentView(R.layout.dialog_log)
                            setTitle("Output")
                            setCancelable(true)
                        }

                        runCatching {
                            lifecycleScope.launch(workerContext) {
                                val isRooted = isRooted()
                                var output = RootUtils.executeWithOutput(newData, "", activity)
                                runSafeOnUiThread {
                                    if (!isRooted) {
                                        output = "### NOT ROOTED ###"
                                    }
                                    val holder = resultDialog.findViewById<TextView>(R.id.log_holder)
                                    holder.text = output
                                }
                            }
                        }
                        resultDialog.show()
                    }
                }
            }.show()
        }

        lifecycleScope.launch {
            val isRooted = isRooted()
            setUpSwipes(isRooted)
        }

        val userType = userPrefs.getInt(K.PREF.USER_TYPE, 1)
        if (userType == 1) {
            binding.sysctl.goAway()
            binding.art.goAway()
            binding.wm.goAway()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            binding.art.goAway()
            binding.wm.goAway()
        }

        if (Build.VERSION.SDK_INT >= 29) {
            binding.art.goAway()
        }
    }

    override fun onResume() {
        super.onResume()
        animViewModel.setAnimRes(R.raw.tune_anim)
    }

    private fun startAnAdvancedActivity(clazz: Class<*>): View.OnClickListener {
        return View.OnClickListener {
            if (isActivityAlive) {
                startActivity(Intent(requireContext(), clazz))
                requireActivity().overridePendingTransition(
                    R.anim.slide_in_right, R.anim.fragment_close_exit
                )
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

    private fun setUpSwipes(isRooted: Boolean) {
        if (isRooted) return

        binding.runAsRoot.setContentEnabled(isRooted)
        binding.ramManager.setContentEnabled(isRooted)
        binding.cpuManager.setContentEnabled(isRooted)
        binding.buildProp.setContentEnabled(isRooted)
        binding.dns.setContentEnabled(isRooted)
        binding.wm.setContentEnabled(isRooted)
        binding.entropy.setContentEnabled(isRooted)
        binding.runAsRoot.setContentEnabled(isRooted)

        binding.warningLayout.show()
        binding.noRootWarning.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            behavior = SwipeDismissBehavior<AppCompatTextView>().apply {
                setSwipeDirection(SwipeDismissBehavior.SWIPE_DIRECTION_ANY)
                listener = object : SwipeDismissBehavior.OnDismissListener {
                    override fun onDismiss(view: View?) {
                        if (binding.warningLayout.childCount == 1) {
                            binding.warningLayout.goAway()
                            view?.goAway()
                        } else {
                            view?.goAway()
                        }
                    }

                    override fun onDragStateChanged(state: Int) {}
                }
            }
        }
    }
}