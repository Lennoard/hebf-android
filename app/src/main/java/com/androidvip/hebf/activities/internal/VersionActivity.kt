package com.androidvip.hebf.activities.internal

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

import com.androidvip.hebf.R
import com.androidvip.hebf.setThemeFromPrefs
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.Themes
import com.androidvip.hebf.utils.UserPrefs

class VersionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setThemeFromPrefs()
        setContentView(R.layout.activity_version)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (UserPrefs(applicationContext).getString(K.PREF.THEME, Themes.LIGHT) == Themes.WHITE) {
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.colorAccentWhite))
            toolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.darkness))
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_theme)
        }
    }
}
