package com.androidvip.hebf.ui.appintro

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils.loadAnimation
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.androidvip.hebf.R
import com.androidvip.hebf.ui.base.BaseActivity
import com.androidvip.hebf.ui.main.MainActivity2
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.Prefs
import com.androidvip.hebf.utils.RootUtils
import com.androidvip.hebf.utils.Utils
import com.androidvip.hebf.views.CustomViewPager
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class AppIntroActivity : BaseActivity() {
    private lateinit var mPager: CustomViewPager
    private lateinit var mPagerAdapter: PagerAdapter
    private lateinit var leftButton: Button
    private lateinit var rightButton: Button
    private lateinit var pb: ProgressBar
    private lateinit var titleTextSwitcher: TextSwitcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utils.setExceptionHandler(this)
        setContentView(R.layout.activity_app_intro)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val tempFolder = K.HEBF.getTempDir(this)
        if (!tempFolder.exists()) {
            if (!tempFolder.mkdirs()) {
                Utils.runCommand("mkdir $tempFolder", "")
            }
        }

        leftButton = findViewById(R.id.intro_button_left)
        rightButton = findViewById(R.id.intro_button_right)
        titleTextSwitcher = findViewById(R.id.intro_switcher_title)

        try {
            titleTextSwitcher.setFactory {
                val textView = TextView(this).apply {
                    textSize = 30f
                    setTextColor(Color.WHITE)
                }

                val layoutParams = FrameLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(30, 0, 4, 20) }

                textView.apply {
                    this.layoutParams = layoutParams
                    typeface = ResourcesCompat.getFont(this@AppIntroActivity, R.font.cairo_semibold)
                }
            }

            val in1 = loadAnimation(this, R.anim.slide_in_left).apply {
                duration = 150
            }
            val out1 = loadAnimation(this, R.anim.slide_out_right).apply {
                duration = 150
            }

            titleTextSwitcher.inAnimation = in1
            titleTextSwitcher.outAnimation = out1

            titleTextSwitcher.setText(getString(R.string.eh_noyz))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        pb = findViewById<ProgressBar>(R.id.intro_progress).apply {
            indeterminateDrawable.setColorFilter(
                    ContextCompat.getColor(this@AppIntroActivity, R.color.colorOnBackground),
                    PorterDuff.Mode.SRC_IN
            )
            isIndeterminate = true
        }

        mPagerAdapter = SliderPageAdapter(supportFragmentManager)

        mPager = findViewById<CustomViewPager>(R.id.intro_pager).apply {
            setPageTransformer(true, SlidePageTransformer())
            adapter = mPagerAdapter
            offscreenPageLimit = 0
        }

        rightButton.setOnClickListener { mPager.currentItem = 1 }

        mPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> {
                        setTitleText(getString(R.string.eh_noyz))
                        leftButton.visibility = View.GONE
                        rightButton.setOnClickListener { mPager.currentItem = 1 }
                    }
                    1 -> {
                        setTitleText(getString(R.string.sobre))
                        leftButton.visibility = View.VISIBLE
                        leftButton.setOnClickListener { mPager.currentItem = 0 }
                        rightButton.setOnClickListener { mPager.currentItem = 2 }
                    }
                    2 -> {
                        setTitleText(getString(R.string.user))
                        rightButton.isEnabled = true
                        leftButton.setOnClickListener { mPager.currentItem = 1 }
                        rightButton.setOnClickListener { mPager.currentItem = 3 }
                    }
                    3 -> {
                        setTitleText(getString(R.string.root_access))
                        leftButton.setOnClickListener { mPager.currentItem = 2 }
                        rightButton.setOnClickListener { mPager.currentItem = 4 }
                        rightButton.isEnabled = false
                        lifecycleScope.launch(workerContext) {
                            while (true) {
                                if (checkedEnvironment) {
                                    runSafeOnUiThread { rightButton.isEnabled = true }
                                    break
                                }
                            }
                        }
                    }
                    4 -> {
                        setTitleText(getString(R.string.storage))
                        leftButton.setOnClickListener { mPager.currentItem = 3 }
                        rightButton.setOnClickListener { mPager.currentItem = 5 }
                        rightButton.isEnabled = false
                        lifecycleScope.launch(workerContext) {
                            while (true) {
                                if (Intro5Fragment.isPermissionGranted) {
                                    runSafeOnUiThread { rightButton.isEnabled = true }
                                    break
                                }
                            }
                        }
                    }
                    5 -> {
                        setTitleText(getString(R.string.finalizar))
                        pb.visibility = View.VISIBLE
                        rightButton.visibility = View.GONE
                        leftButton.setOnClickListener { mPager.currentItem = 4 }
                        rightButton.setOnClickListener {
                            val prefs = Prefs(this@AppIntroActivity)
                            prefs.putBoolean(K.PREF.IS_FIRST_START, false)
                            val i = Intent(this@AppIntroActivity, MainActivity2::class.java)
                            startActivity(i)
                            finish()
                        }
                        lifecycleScope.launch(workerContext) {
                            val filesDir = filesDir.toString()
                            if (isRooted) {
                                RootUtils.execute("chmod 755 $filesDir/zipalign", this@AppIntroActivity)
                                RootUtils.copyFile("$filesDir/zipalign", "/system/bin/zipalign")
                                RootUtils.deleteFileOrDir("$filesDir/zipalign")
                            }

                            Utils.copyAssets(this@AppIntroActivity)

                            runSafeOnUiThread {
                                if (isRooted) {
                                    val checkFile = File(filesDir, "hebf.hebf")
                                    try {
                                        if (!checkFile.createNewFile())
                                            runCommand("touch $checkFile")
                                    } catch (e: IOException) {
                                        runCommand("touch $checkFile")
                                    }

                                }
                                pb.visibility = View.GONE
                                rightButton.visibility = View.VISIBLE
                                rightButton.text = getString(R.string.done)
                                rightButton.isEnabled = true
                            }
                        }
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({ rightButton.visibility = View.VISIBLE }, 1500)
    }

    private fun setTitleText(text: String) {
        try {
            titleTextSwitcher.setText(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private inner class SliderPageAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(
        fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
    ) {
        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> Intro0Fragment()
                1 -> Intro1Fragment()
                2 -> Intro2Fragment()
                3 -> Intro4Fragment()
                4 -> Intro5Fragment()
                5 -> Intro6Fragment()
                else -> Intro0Fragment()
            }
        }

        override fun getCount(): Int {
            return NUM_PAGES
        }
    }

    private inner class SlidePageTransformer : ViewPager.PageTransformer {
        override fun transformPage(view: View, position: Float) {
            val pageWidth = view.width
            when {
                position < -1 -> view.alpha = 0f
                position <= 0 -> {
                    view.alpha = 1f
                    view.translationX = 0f
                    view.scaleX = 1f
                    view.scaleY = 1f
                }
                position <= 1 -> {
                    view.alpha = 1 - position
                    view.translationX = pageWidth * -position
                }
                else -> view.alpha = 0f
            }
        }
    }

    companion object {
        private const val NUM_PAGES = 6
        internal var isRooted = false
        internal var checkedEnvironment = false

        fun checkRootStatus(hasRootAccess: Boolean) {
            isRooted = hasRootAccess
        }

        fun setCheckedEnvironment(checked: Boolean) {
            checkedEnvironment = checked
        }
    }
}
