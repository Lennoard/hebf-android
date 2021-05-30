package com.androidvip.hebf.views

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.androidvip.hebf.R
import java.util.concurrent.atomic.AtomicInteger

class SeekBarWithIntervals(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {
    private var linearLayout: LinearLayout? = null
    private var internalSeekBar: SeekBar? = null

    private var widthMeasureSpec = 0
    private var heightMeasureSpec = 0
    private var isAlignmentResetOnLayoutChange: Boolean = false

    private val activity: Activity
        get() = context as Activity

    private val seekBarThumbWidth: Int
        get() = resources.getDimensionPixelOffset(R.dimen.medium_margin)

    var progress: Int
        get() = seekBar!!.progress
        set(progress) {
            seekBar!!.progress = progress
        }

    val seekBar: SeekBar?
        get() {
            if (internalSeekBar == null) {
                internalSeekBar = findViewById(R.id.seekbar)
            }
            return internalSeekBar
        }


    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.seekbar_with_intervals, this)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        if (changed) {
            if (!isAlignmentResetOnLayoutChange) {
                alignIntervals()

                // We've changed the intervals layout, we need to refresh.
                linearLayout!!.measure(widthMeasureSpec, heightMeasureSpec)
                linearLayout!!.layout(linearLayout!!.left, linearLayout!!.top, linearLayout!!.right, linearLayout!!.bottom)
            }
        }
    }

    private fun alignIntervals() {
        if (seekBar != null) {
            val widthOfSeekBarThumb = seekBarThumbWidth
            val thumbOffset = widthOfSeekBarThumb / 2

            alignFirstInterval(thumbOffset)
            alignIntervalsInBetween()
            alignLastInterval()
            isAlignmentResetOnLayoutChange = true
        }
    }

    private fun alignFirstInterval(offset: Int) {
        val firstInterval = getLinearLayout()!!.getChildAt(0) as TextView
        firstInterval.setPadding(offset, 0, 0, 0)
        firstInterval.gravity = Gravity.START
    }

    private fun alignIntervalsInBetween() {
        for (index in 1 until getLinearLayout()!!.childCount - 1) {
            val textViewInterval = getLinearLayout()!!.getChildAt(index) as TextView

            when (index) {
                1 -> {
                    textViewInterval.gravity = Gravity.START
                    textViewInterval.setPadding(16.dp, 0, 0, 0)
                }
                2 -> textViewInterval.gravity = Gravity.CENTER_HORIZONTAL
                3 -> {
                    textViewInterval.gravity = Gravity.END
                    textViewInterval.setPadding(0, 0, 16.dp, 0)
                }
            }
        }

    }

    private fun alignLastInterval() {
        val lastIndex = getLinearLayout()!!.childCount - 1

        val lastInterval = getLinearLayout()!!.getChildAt(lastIndex) as TextView
        lastInterval.gravity = Gravity.END

        lastInterval.setPadding(0, 0, 16.dp, 0)
    }

    @Synchronized
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        this.widthMeasureSpec = widthMeasureSpec
        this.heightMeasureSpec = heightMeasureSpec

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    fun setIntervals(intervals: List<String>) {
        displayIntervals(intervals)
        seekBar!!.max = intervals.size - 1
    }

    private fun displayIntervals(intervals: List<String>) {
        var idOfPreviousInterval = 0

        if (getLinearLayout()!!.childCount == 0) {
            for (interval in intervals) {
                val textViewInterval = createInterval(interval)
                alignTextViewToRightOfPreviousInterval(textViewInterval, idOfPreviousInterval)

                idOfPreviousInterval = textViewInterval.id

                getLinearLayout()!!.addView(textViewInterval)
            }
        }
    }

    private fun createInterval(interval: String): TextView {
        val textBoxView = LayoutInflater.from(context).inflate(R.layout.seekbar_with_intervals_labels, null)

        val textView = textBoxView.findViewById<TextView>(R.id.textViewInterval)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            textView.id = View.generateViewId()
        else
            textBoxView.id = generateViewIdInternal()

        textView.text = interval

        return textView
    }

    private fun alignTextViewToRightOfPreviousInterval(textView: TextView, idOfPreviousInterval: Int) {
        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT)

        params.weight = 1F

        textView.layoutParams = params
    }

    fun setOnSeekBarChangeListener(onSeekBarChangeListener: SeekBar.OnSeekBarChangeListener) {
        seekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val typedValueSelected = TypedValue()
                val typedValueNormal = TypedValue()
                (context as Activity).theme.resolveAttribute(R.attr.colorAccent, typedValueSelected, true)
                (context as Activity).theme.resolveAttribute(R.attr.colorOnPrimary, typedValueNormal, true)

                for (i in 0 until getLinearLayout()!!.childCount) {
                    val tv = getLinearLayout()!!.getChildAt(i) as TextView
                    if (i == seekBar.progress) {
                        tv.typeface = Typeface.DEFAULT_BOLD
                        tv.setTextColor(resources.getColor(typedValueSelected.resourceId))
                    } else {
                        tv.typeface = Typeface.DEFAULT
                        tv.setTextColor(resources.getColor(typedValueNormal.resourceId))
                    }
                }
                onSeekBarChangeListener.onProgressChanged(seekBar, progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                onSeekBarChangeListener.onStartTrackingTouch(seekBar)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onSeekBarChangeListener.onStopTrackingTouch(seekBar)
            }
        })

    }

    private fun getLinearLayout(): LinearLayout? {

        if (linearLayout == null) {
            linearLayout = findViewById(R.id.intervals)
        }

        return linearLayout
    }

    private val Int.dp: Int
        get() {
            var px = this
            try {
                px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics).toInt()
            } catch (ignored: Exception) {
            }
            return px
        }

    companion object {
        private val sNextGeneratedId = AtomicInteger(1)

        /**
         * Generate a value suitable for use in [.setId].
         * This value will not collide with ID values generated at build time by aapt for R.id.
         *
         * @return a generated ID value
         */
        fun generateViewIdInternal(): Int {
            while (true) {
                val result = sNextGeneratedId.get()
                // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
                var newValue = result + 1
                if (newValue > 0x00FFFFFF) newValue = 1 // Roll over to 1, not 0.
                if (sNextGeneratedId.compareAndSet(result, newValue)) {
                    return result
                }
            }
        }
    }
}
