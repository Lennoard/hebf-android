package com.androidvip.hebf.ui.main.battery

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.androidvip.hebf.R
import com.androidvip.hebf.activities.MainActivity
import com.androidvip.hebf.databinding.FragmentBatteryBinding
import com.androidvip.hebf.databinding.FragmentDashboardBinding
import com.androidvip.hebf.ui.base.binding.BaseViewBindingFragment

class BatteryFragment : BaseViewBindingFragment<FragmentBatteryBinding>(
    FragmentBatteryBinding::inflate
) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}