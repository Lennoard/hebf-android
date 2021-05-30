package com.androidvip.hebf.ui.internal

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.androidvip.hebf.R

class ThanksActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_thanks)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val link1 = findViewById<TextView>(R.id.special_sub)
        val link2 = findViewById<TextView>(R.id.special_sub_1)

        link1.movementMethod = LinkMovementMethod.getInstance()
        link2?.movementMethod = LinkMovementMethod.getInstance()
    }
}
