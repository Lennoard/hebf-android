package com.androidvip.hebf.ui.prefs

import android.R
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import com.androidvip.hebf.BuildConfig
import com.androidvip.hebf.databinding.FragmentBugReport2Binding
import com.androidvip.hebf.goAway
import com.androidvip.hebf.show
import com.androidvip.hebf.toast
import com.androidvip.hebf.ui.base.binding.BaseViewBindingFragment
import com.androidvip.hebf.utils.Logger.logWTF
import com.topjohnwu.superuser.Shell

class BugReportFragment : BaseViewBindingFragment<FragmentBugReport2Binding>(
    FragmentBugReport2Binding::inflate
) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) binding.pp.text = Html.fromHtml(
            "<a href=\"https://hebfoptimizer.androidvip.com.br/terms/privacy-policy.html\">Privacy policy</a>",
            Html.FROM_HTML_MODE_LEGACY
        ) else binding.pp.text = Html.fromHtml("<a href=\"https://hebfoptimizer.androidvip.com.br/terms/privacy-policy.html\">Privacy policy</a>")

        binding.pp.movementMethod = LinkMovementMethod.getInstance()

        val androidVersions = arrayOf(
            "4.1", "4.2", "4.3", "4.4", "5.0", "5.1",
            "6.0", "7.0", "7.1", "8.0", "8.1", "9.0", "10", "R", "S"
        )

        val versionAdapter = ArrayAdapter(findContext(), R.layout.simple_spinner_item, androidVersions)
        versionAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spinnerOs.adapter = versionAdapter

        val reportType = arrayOf("Bug", "Suggestion", "Other")

        val typeAdapter = ArrayAdapter(findContext(), R.layout.simple_spinner_item, reportType)
        typeAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.typeSpinner.adapter = typeAdapter

        binding.showMore.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.typeLayout.show()
                binding.androidVersionLayout.show()
                binding.checkCustom.show()
            } else {
                binding.typeLayout.goAway()
                binding.androidVersionLayout.goAway()
                binding.checkCustom.goAway()
            }
        }

        binding.send.setOnClickListener {
            if (checkFields()) {
                send()
            }
        }

        binding.checkCustom.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.textInputLayoutWhich.show()
            } else {
                binding.textInputLayoutWhich.goAway()
            }
        }

        binding.which.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND && checkFields()) {
                send()
            }
            true
        }
    }

    private fun send() {
        val body = if (binding.showMore.isChecked) buildString {
            appendLine(binding.editBody.text.toString())
            appendLine()
            appendLine("Android version: ${binding.spinnerOs.selectedItem}")
            appendLine("Type: ${binding.typeSpinner.selectedItem}")
            appendLine("IsCustomROM: ${binding.checkCustom.isChecked}")
            appendLine("ROM: ${binding.which.text}")
            appendLine("==================================")
            appendLine("Device: ${Build.MODEL}, ${Build.DEVICE} (${Build.BRAND})")
            appendLine("Product: ${Build.PRODUCT}")
            appendLine("Board: ${Build.BOARD}")
            appendLine("Build type: ${Build.TYPE}")
            appendLine("Rooted: ${Shell.rootAccess()}")
            appendLine("==================================")
            appendLine("HEBF version: v${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})")
            appendLine("Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        } else buildString {
            appendLine(binding.editBody.text.toString())
            appendLine()
            appendLine("Device: ${Build.MODEL}, ${Build.DEVICE} (${Build.BRAND})")
            appendLine("Product: ${Build.PRODUCT}")
            appendLine("Build type: ${Build.TYPE}")
            appendLine("Rooted: ${Shell.rootAccess()}")
            appendLine("==================================")
            appendLine("HEBF version: v${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})")
            appendLine("Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        }

        val sendEmailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@androidvip.com.br"))
            putExtra(Intent.EXTRA_SUBJECT, "HEBF Optimizer")
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            startActivity(Intent.createChooser(sendEmailIntent, "Send mail..."))
            Toast.makeText(context, "Choose your email client", Toast.LENGTH_LONG).show()
        } catch (ex: ActivityNotFoundException) {
            logWTF("There are no email clients installed! " + ex.message, context)
            requireContext().toast("There are no email clients installed!")
        }
    }

    private fun checkFields(): Boolean {
        val body = binding.editBody.text.toString()
        return (body.length >= 10 && body.isNotBlank()).also {
            binding.textInputLayout.error = if (it) null else "Please write a more detailed message"
        }
    }

}