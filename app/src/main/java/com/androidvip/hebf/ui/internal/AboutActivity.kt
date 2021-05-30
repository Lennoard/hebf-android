package com.androidvip.hebf.ui.internal

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.androidvip.hebf.R
import com.androidvip.hebf.createVectorDrawable
import com.androidvip.hebf.databinding.ActivityAboutBinding
import com.androidvip.hebf.helpers.HebfApp
import com.androidvip.hebf.ui.base.binding.BaseViewBindingActivity
import com.androidvip.hebf.utils.Logger
import com.androidvip.hebf.utils.Utils
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity

class AboutActivity : BaseViewBindingActivity<ActivityAboutBinding>(ActivityAboutBinding::inflate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(createVectorDrawable(R.drawable.ic_arrow_back))
        }

        binding.translateCard.setOnClickListener {
            startActivity(Intent(this, TranslateActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_about, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_libraries -> {
                OssLicensesMenuActivity.setActivityTitle(getString(R.string.license))
                startActivity(Intent(this, OssLicensesMenuActivity::class.java))
            }
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
