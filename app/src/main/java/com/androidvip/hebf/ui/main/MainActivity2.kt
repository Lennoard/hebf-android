package com.androidvip.hebf.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.ViewCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.androidvip.hebf.R
import com.androidvip.hebf.activities.MainActivity
import com.androidvip.hebf.activities.internal.AboutActivity
import com.androidvip.hebf.databinding.ActivityMain2Binding
import com.androidvip.hebf.ui.base.binding.BaseViewBindingActivity

class MainActivity2 : BaseViewBindingActivity<ActivityMain2Binding>(ActivityMain2Binding::inflate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with (binding.toolbar) {
            setSupportActionBar(this)
            ViewCompat.setElevation(this, 0F)
        }

        val host = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigationDashboard,
                R.id.navigationBattery,
                R.id.navigationPerformance,
                R.id.navigationTools,
                R.id.navigationTune
            )
        )
        binding.collapsingLayout.setupWithNavController(
            binding.toolbar, host.navController, appBarConfiguration
        )
        binding.navView.setupWithNavController(host.navController)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionSettings -> startActivity(Intent(this, AboutActivity::class.java))
            R.id.actionDeviceInfo -> startActivity(Intent(this, MainActivity::class.java))
            R.id.actionMyAccount -> startActivity(Intent(this, AboutActivity::class.java))
            R.id.actionAbout -> startActivity(Intent(this, AboutActivity::class.java))
        }
        return true
    }
}