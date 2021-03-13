package com.androidvip.hebf.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.androidvip.hebf.R
import com.androidvip.hebf.activities.LmkActivity
import com.androidvip.hebf.activities.VmActivity
import kotlinx.android.synthetic.main.fragment_ram_manager.*

class RamManagerFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ram_manager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lmkTriggerCard.setOnClickListener {
            startActivity(Intent(findContext(), LmkActivity::class.java))
            activity?.overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out)
        }

        vmTriggerCard.setOnClickListener {
            startActivity(Intent(findContext(), VmActivity::class.java))
            activity?.overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out)
        }
    }
}