package com.androidvip.hebf.appintro

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.AppCompatRadioButton
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.androidvip.hebf.R
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.Themes
import com.androidvip.hebf.utils.UserPrefs

class Intro3Fragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_intro3, container, false) as ViewGroup
        val light: AppCompatRadioButton = rootView.findViewById(R.id.radio_light)
        val dark: AppCompatRadioButton = rootView.findViewById(R.id.radio_dark)
        val systemDefault: AppCompatRadioButton = rootView.findViewById(R.id.radio_system_default)

        val userPrefs = UserPrefs(requireContext().applicationContext)

        systemDefault.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                userPrefs.putString(K.PREF.THEME, Themes.SYSTEM_DEFAULT)
                dark.isChecked = false
                light.isChecked = false
            }
        }

        light.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                userPrefs.putString(K.PREF.THEME, Themes.LIGHT)
                dark.isChecked = false
                systemDefault.isChecked = false
            }
        }

        dark.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                userPrefs.putString(K.PREF.THEME, Themes.DARKNESS)
                light.isChecked = false
                systemDefault.isChecked = false
            }
        }

        return rootView
    }

}