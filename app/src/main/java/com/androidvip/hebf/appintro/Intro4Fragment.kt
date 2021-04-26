package com.androidvip.hebf.appintro

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.*
import com.androidvip.hebf.ui.base.BaseFragment
import com.androidvip.hebf.helpers.HebfApp
import com.androidvip.hebf.utils.Utils
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Intro4Fragment : BaseFragment() {
    private lateinit var resultIcon: AppCompatImageView
    private lateinit var checkingEnvironmentText: TextView
    private lateinit var text: TextView
    private lateinit var pb: ProgressBar
    private var animIn = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_intro4, container, false) as ViewGroup

        resultIcon = rootView.findViewById(R.id.check_result)
        checkingEnvironmentText = rootView.findViewById(R.id.checking_text)
        pb = rootView.findViewById(R.id.progress_checking)
        text = rootView.findViewById(R.id.intoRootDescription)

        return rootView
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch (workerContext) {
            delay(1000)

            val isRooted = Shell.rootAccess()
            val isBusyboxInstalled = Utils.runCommand("which busybox", "").isNotEmpty()
            runSafeOnUiThread {
                updateUi(isRooted, isBusyboxInstalled)
            }
        }
    }

    private fun updateUi(isRooted: Boolean, isBusyboxInstalled: Boolean) {
        if (isRooted) {
            if (!isBusyboxInstalled) {
                text.setTextColor(ContextCompat.getColor(findContext(), R.color.colorWarning))
                text.text = getString(R.string.busybox_not_found)
            }
            checkingEnvironmentText.goAway()
            resultIcon.setImageResource(R.drawable.ohyeah)
            AppIntroActivity.checkRootStatus(isRooted)
            AppIntroActivity.setCheckedEnvironment(true)
        } else {
            checkingEnvironmentText.goAway()

            text.setText(R.string.app_intro_rootless_not_supported)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val avdIn = ContextCompat.getDrawable(
                    requireContext(), R.drawable.avd_rootless
                )
                val avdOut: AnimatedVectorDrawable = ContextCompat.getDrawable(
                    requireContext(), R.drawable.avd_rootless_out
                ) as AnimatedVectorDrawable

                if (avdIn is AnimatedVectorDrawable) {
                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed(object : Runnable {
                        override fun run() {
                            if (isActivityAlive) {
                                resultIcon.setImageDrawable(avdOut)
                                animIn = if (animIn) {
                                    avdOut.start()
                                    false
                                } else {
                                    resultIcon.setImageDrawable(avdIn)
                                    avdIn.start()
                                    true
                                }
                                handler.postDelayed(this, 2000)
                            }
                        }
                    }, 2000)
                    resultIcon.setImageDrawable(avdIn)
                    avdIn.start()
                    animIn = true
                }
            } else {
                resultIcon.setImageResource(R.drawable.ic_close)
            }
        }
        pb.goAway()
        resultIcon.show()
    }
}