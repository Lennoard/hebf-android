package com.androidvip.hebf.activities.internal

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.androidvip.hebf.R
import com.androidvip.hebf.setThemeFromPrefs
import com.androidvip.hebf.utils.Utils
import kotlinx.android.synthetic.main.activity_busybox_installer.*

class BusyboxInstallerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setThemeFromPrefs()
        setContentView(R.layout.activity_busybox_installer)

        busyboxButtonGooglePlay.setOnClickListener {
            Utils.webPage(this, "https://play.google.com/store/search?q=busybox&c=apps")
        }
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
