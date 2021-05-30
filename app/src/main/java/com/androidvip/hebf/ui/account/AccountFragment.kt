package com.androidvip.hebf.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.androidvip.hebf.R
import com.androidvip.hebf.databinding.FragmentAccountBinding
import com.androidvip.hebf.helpers.HebfApp
import com.androidvip.hebf.ui.base.binding.BaseViewBindingFragment
import com.androidvip.hebf.ui.data.BackupActivity
import com.androidvip.hebf.ui.data.ImportDataActivity
import com.androidvip.hebf.utils.ContextBottomSheet
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.SheetOption
import com.androidvip.hebf.utils.Utils
import java.io.File
import java.util.*

class AccountFragment : BaseViewBindingFragment<FragmentAccountBinding>(
    FragmentAccountBinding::inflate
) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val achievements = userPrefs.getStringSet(K.PREF.ACHIEVEMENT_SET, HashSet())
        binding.achievements.text = when {
            achievements.isEmpty() -> getString(R.string.none)
            achievements.size == 7 -> "$achievements You got them all!"
            else -> achievements.toString()
        }

        binding.restoreLayout.setOnClickListener { showRestoreSheet() }
        binding.backupLayout.setOnClickListener {
            startActivity(Intent(requireContext(), BackupActivity::class.java))
        }

    }

    override fun onStart() {
        super.onStart()
        populate()
    }

    private fun populate() {
        val backups = File(requireContext().filesDir, "backups").listFiles()

        if (backups != null) {
            binding.restoreBackup.text = resources.getQuantityString(
                R.plurals.backups_count, backups.size, backups.size
            )
        }
    }

    private fun showRestoreSheet() {
        val menuOptions = arrayListOf(
            SheetOption(
                getString(R.string.local_backup), "local", R.drawable.ic_backup_local
            ),
            SheetOption(getString(R.string.cloud_backup), "cloud", R.drawable.ic_backup)
        )

        val contextSheet =  ContextBottomSheet.newInstance(getString(R.string.restore), menuOptions)
        contextSheet.onOptionClickListener = object : ContextBottomSheet.OnOptionClickListener {
            override fun onOptionClick(tag: String) {
                contextSheet.dismiss()
                when (tag) {
                    "local" -> {
                        Intent(findContext(), ImportDataActivity::class.java).apply {
                            putExtra(K.EXTRA_RESTORE_ACTIVITY, true)
                            startActivity(this)
                        }
                    }

                    "cloud" -> throw UnsupportedOperationException()
                }
            }
        }

        contextSheet.show((requireActivity() as AppCompatActivity).supportFragmentManager, "sheet")
    }
}