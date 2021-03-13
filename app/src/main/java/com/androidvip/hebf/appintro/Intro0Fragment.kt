package com.androidvip.hebf.appintro

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.androidvip.hebf.R

class Intro0Fragment : Fragment() {
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_intro0, container, false)

        val img = rootView.findViewById<FrameLayout>(R.id.splashIconLayout)
        val txt = rootView.findViewById<LinearLayout>(R.id.splashAppNameLayout)

        handler.postDelayed({ txt.visibility = View.VISIBLE }, 1200)
        handler.postDelayed({ img.visibility = View.VISIBLE }, 100)

        return rootView
    }
}
