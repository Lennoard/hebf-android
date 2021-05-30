package com.androidvip.hebf.ui.appintro

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.androidvip.hebf.R

class Intro0Fragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_intro0, container, false)

        val img = rootView.findViewById<FrameLayout>(R.id.splashIconLayout)
        val txt = rootView.findViewById<LinearLayout>(R.id.splashAppNameLayout)

        val handler2 = Handler()
        handler2.postDelayed({ txt.visibility = View.VISIBLE }, 1200)

        val handler = Handler()
        handler.postDelayed({ img.visibility = View.VISIBLE }, 100)

        return rootView
    }
}
