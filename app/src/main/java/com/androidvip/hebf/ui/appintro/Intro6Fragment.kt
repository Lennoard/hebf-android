package com.androidvip.hebf.ui.appintro

import android.graphics.drawable.Animatable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import com.androidvip.hebf.R

class Intro6Fragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_intro6, container, false)
        activity?.let {
            if (!it.isFinishing) it.title = getString(R.string.finalizar)
        }

        val imgCheck = rootView.findViewById<ImageView>(R.id.img_finalizar)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            runCatching {
                val drawable = imgCheck.drawable
                if (drawable is Animatable) {
                    val handler = Handler()
                    handler.postDelayed(object : Runnable {
                        override fun run() {
                            (drawable as Animatable).start()
                            handler.postDelayed(this, 3000)
                        }
                    }, 2000)
                }
            }
        }
        return rootView
    }

}