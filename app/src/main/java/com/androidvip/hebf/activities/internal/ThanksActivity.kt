package com.androidvip.hebf.activities.internal

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.text.method.LinkMovementMethod
import android.widget.TextView

import com.androidvip.hebf.R
import com.androidvip.hebf.setThemeFromPrefs
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.Themes
import com.androidvip.hebf.utils.UserPrefs

class ThanksActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setThemeFromPrefs()

        setContentView(R.layout.activity_thanks)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (UserPrefs(applicationContext).getString(K.PREF.THEME, Themes.LIGHT) == Themes.WHITE) {
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.colorAccentWhite))
            toolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.darkness))
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_theme)
        }

        val link1 = findViewById<TextView>(R.id.special_sub)
        val link2 = findViewById<TextView>(R.id.special_sub_1)

        link1.movementMethod = LinkMovementMethod.getInstance()
        link2?.movementMethod = LinkMovementMethod.getInstance()
    }
}
