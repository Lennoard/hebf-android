package com.androidvip.hebf.activities.internal

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.androidvip.hebf.R
import com.androidvip.hebf.activities.BaseActivity
import com.androidvip.hebf.utils.*
import com.google.android.material.appbar.CollapsingToolbarLayout
import kotlinx.android.synthetic.main.activity_about.*
import kotlinx.android.synthetic.main.content_about.*

class AboutActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_about)
        setUpToolbar(toolbar)

        if (UserPrefs(applicationContext).getString(K.PREF.THEME, Themes.SYSTEM_DEFAULT) == Themes.WHITE) {
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.colorAccentWhite))
            toolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.darkness))
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_theme)

            val collapsingToolbarLayout = findViewById<CollapsingToolbarLayout>(R.id.collapsing_app_bar_layout)
            collapsingToolbarLayout.setCollapsedTitleTextColor(ContextCompat.getColor(this, R.color.colorAccentWhite))
            collapsingToolbarLayout.setExpandedTitleColor(ContextCompat.getColor(this, R.color.colorAccentWhite))
        }

        fab.setOnClickListener {
            Utils.webPage(this, "http://forum.xda-developers.com/android/software-hacking/hebf-tweaking-battery-saver-optimizer-t3401341")
        }
        fab.setOnLongClickListener {
            Toast.makeText(this, "XDA Thread", Toast.LENGTH_SHORT).show()
            false
        }

        translateButton.setOnClickListener {
            startActivity(Intent(this, TranslateActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_about, menu)
        if (UserPrefs(applicationContext).getString(K.PREF.THEME, Themes.SYSTEM_DEFAULT) == Themes.WHITE) {
            for (i in 0 until menu.size()) {
                val menuItem = menu.getItem(i)
                if (menuItem != null) {
                    val iconDrawable = menuItem.icon
                    if (iconDrawable != null) {
                        iconDrawable.mutate()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            iconDrawable.setTint(ContextCompat.getColor(this, R.color.colorAccentWhite))
                        } else {
                            iconDrawable.setColorFilter(ContextCompat.getColor(this, R.color.colorAccentWhite), PorterDuff.Mode.SRC_IN)
                        }
                    }
                }
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_libraries -> Utils.webDialog(this, "https://hebfoptimizer.androidvip.com.br/terms/lic.html")
            R.id.action_version -> startActivity(Intent(this, VersionActivity::class.java))
            R.id.action_thanks -> startActivity(Intent(this, ThanksActivity::class.java))
            R.id.action_translate -> startActivity(Intent(this, TranslateActivity::class.java))
        }
        return true
    }

    override fun onBackPressed() {
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fragment_close_enter, R.anim.fragment_close_exit)
    }

    fun xdaIv(view: View) {
        Utils.webPage(this, "http://forum.xda-developers.com/member.php?u=5968361")
    }

    fun xdaL(view: View) {
        Utils.webPage(this, "http://forum.xda-developers.com/member.php?u=6652564")
    }

    fun gitHubL(view: View) {
        Utils.webPage(this, "https://github.com/Lennoard")
    }

    fun mailL(view: View) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("lennoardrai@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "HEBF Optimizer (About)")
        }

        try {
            startActivity(Intent.createChooser(intent, "Send mail..."))
            Toast.makeText(this, "Choose your email client", Toast.LENGTH_LONG).show()
        } catch (ex: ActivityNotFoundException) {
            Logger.logWTF("There are no email clients installed! ${ex.message}", this)
        }
    }
}
