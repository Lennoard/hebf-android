package com.androidvip.hebf.ui.main.tune

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.androidvip.hebf.R
import com.androidvip.hebf.databinding.FragmentBatteryBinding
import com.androidvip.hebf.databinding.FragmentTuneBinding
import com.androidvip.hebf.ui.base.binding.BaseViewBindingFragment

class TuneFragment : BaseViewBindingFragment<FragmentTuneBinding>(
    FragmentTuneBinding::inflate
) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}