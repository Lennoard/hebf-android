package com.androidvip.hebf.ui.main.notif

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.*
import com.androidvip.hebf.databinding.FragmentNotificationsBinding
import com.androidvip.hebf.helpers.HebfApp
import com.androidvip.hebf.ui.base.binding.BaseViewBindingFragment
import com.androidvip.hebf.ui.internal.BusyboxInstallerActivity
import com.androidvip.hebf.ui.main.LottieAnimViewModel
import com.androidvip.hebf.ui.main.NotificationViewModel
import com.androidvip.hebf.utils.RootUtils
import com.androidvip.hebf.utils.ext.launchUrl
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.*

class NotificationsFragment : BaseViewBindingFragment<FragmentNotificationsBinding>(
    FragmentNotificationsBinding::inflate
) {
    private val animViewModel: LottieAnimViewModel by sharedViewModel()
    private val notificationViewModel: NotificationViewModel by sharedViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch(workerContext) {
            val isBusyboxAvailable = RootUtils.executeSync("which busybox").isNotEmpty()
            val isRooted = isRooted()
            val imgBitmap = ANDRE_IMG_URL.getBitMapFromUrl()

            runSafeOnUiThread {
                imgBitmap?.let {
                    binding.helpAndreImg.setImageBitmap(it)
                }

                if (!isBusyboxAvailable && isRooted) {
                    binding.busyboxNotFoundCard.show()
                    binding.noNotifications.goAway()
                    binding.busyboxInstallButton.setOnClickListener {
                        startActivity(Intent(findContext(), BusyboxInstallerActivity::class.java))
                    }
                }
            }
        }

        val dismissedHelpAndre = userPrefs.getBoolean(PREF_HELP_ANDRE, false)
        if (!dismissedHelpAndre) {
            binding.noNotifications.goAway()
            binding.helpAndreCard.show()
        } else {
            binding.helpAndreCard.goAway()
        }

        binding.helpAndreHelp.setOnClickListener {
            userPrefs.putBoolean(PREF_HELP_ANDRE, true)
            val language = Locale.getDefault().language
            val url = if (language == "pt") {
                "https://androidvip.com.br/etc/ajude-andre.html"
            } else {
                "https://androidvip.com.br/etc/help-andre.html"
            }

            requireContext().launchUrl(url)
            binding.helpAndreCard.goAway()
            notificationViewModel.decrement()
        }

        binding.helpAndreDismiss.setOnClickListener {
            userPrefs.putBoolean(PREF_HELP_ANDRE, true)
            binding.helpAndreCard.goAway()
            notificationViewModel.decrement()
        }
    }

    override fun onResume() {
        super.onResume()
        animViewModel.setAnimRes(0)
    }

    companion object {
        const val PREF_HELP_ANDRE = "help_andre_card"
        private const val ANDRE_IMG_URL = "https://static.vakinha.com.br/uploads/ckeditor/pictures/49371/content_DSC00215.JPG"
    }
}