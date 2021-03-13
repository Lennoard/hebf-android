package com.androidvip.hebf.appintro

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.AppCompatRadioButton
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.androidvip.hebf.R
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.UserPrefs

class Intro2Fragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_intro2, container, false)
        val normal: AppCompatRadioButton = rootView.findViewById(R.id.intro_radio_user_normal)
        val chuck: AppCompatRadioButton = rootView.findViewById(R.id.intro_radio_user_norris)

        val userPrefs = UserPrefs(requireActivity().applicationContext)

        normal.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                userPrefs.putInt(K.PREF.USER_TYPE, 1)
                chuck.isChecked = false
            }
        }

        chuck.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                userPrefs.putInt(K.PREF.USER_TYPE, 3)
                normal.isChecked = false
            }
        }

        return rootView
    }

}