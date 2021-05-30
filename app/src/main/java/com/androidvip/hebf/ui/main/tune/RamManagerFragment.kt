package com.androidvip.hebf.ui.main.tune

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.R
import com.androidvip.hebf.databinding.FragmentRamManager2Binding
import com.androidvip.hebf.goAway
import com.androidvip.hebf.show
import com.androidvip.hebf.ui.base.binding.BaseViewBindingFragment
import com.androidvip.hebf.ui.main.LottieAnimViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class RamManagerFragment : BaseViewBindingFragment<FragmentRamManager2Binding>(
    FragmentRamManager2Binding::inflate
) {
    private val animViewModel: LottieAnimViewModel by sharedViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            val isRooted = isRooted()
            delay(300)

            binding.progress.goAway()

            if (!isRooted) {
                binding.noRootWarning.show()
            } else {
                binding.scrollView.show()
            }

            binding.lmk.setOnClickListener {
                startActivity(Intent(requireContext(), LmkActivity::class.java))
            }

            binding.vm.setOnClickListener {
                startActivity(Intent(requireContext(), VmActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        animViewModel.setAnimRes(R.raw.tune_anim)
    }
}