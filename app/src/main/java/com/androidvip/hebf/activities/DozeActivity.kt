package com.androidvip.hebf.activities

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.androidvip.hebf.R
import com.androidvip.hebf.fragments.doze.DozeInfoFragment
import com.androidvip.hebf.fragments.doze.DozeSettingsFragment
import com.androidvip.hebf.fragments.doze.DozeWhitelistFragment
import com.androidvip.hebf.utils.Logger
import kotlinx.android.synthetic.main.activity_doze.*

@RequiresApi(api = Build.VERSION_CODES.M)
class DozeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doze)

        setUpToolbar(toolbar)
        setupViewPager(dozeViewPager)

        dozeTabLayout.setupWithViewPager(dozeViewPager)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Toast.makeText(this, "FOR MARSHMALLOW+ ONLY", Toast.LENGTH_SHORT).show()
            Logger.logWTF("Tried to open Doze settings in API < 23", this)
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupViewPager(viewPager: ViewPager) {
        viewPager.adapter = ViewPagerAdapter(supportFragmentManager).apply {
            addFragment(DozeSettingsFragment(), getString(R.string.settings))
            addFragment(DozeWhitelistFragment(), getString(R.string.doze_whitelist))
            addFragment(DozeInfoFragment(), getString(R.string.summary))
        }
    }

    override fun onBackPressed() {
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fragment_close_enter, android.R.anim.slide_out_right)
    }

    private class ViewPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        private val fragmentList = mutableListOf<Fragment>()
        private val fragmentTitleList = mutableListOf<String>()

        override fun getItem(position: Int): Fragment {
            return fragmentList[position]
        }

        override fun getCount(): Int {
            return fragmentList.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return fragmentTitleList[position]
        }

        fun addFragment(fragment: Fragment, title: String) {
            fragmentList.add(fragment)
            fragmentTitleList.add(title)
        }
    }
}