package com.androidvip.hebf.views

import android.content.Context
import android.util.AttributeSet
import android.widget.*
import androidx.appcompat.widget.AppCompatTextView
import com.androidvip.hebf.R

class InfoSeekBar(context: Context, attrs: AttributeSet): RelativeLayout(context, attrs) {
    interface OnSeekBarChangeListener {
        fun onProgressChanged(infoSeekBar: InfoSeekBar, progress: Int, fromUser: Boolean)
        fun onStartTrackingTouch(infoSeekBar: InfoSeekBar)
        fun onStopTrackingTouch(infoSeekBar: InfoSeekBar)
    }

    val seekBar: SeekBar by lazy { findViewById<SeekBar>(R.id.infoSeekbar) }
    private val statusTextView: AppCompatTextView by lazy { findViewById<AppCompatTextView>(R.id.infoSeekbarStatusText) }
    private val titleTextView: AppCompatTextView by lazy { findViewById<AppCompatTextView>(R.id.infoSeekbarTitle) }
    private val descriptionTextView: AppCompatTextView by lazy { findViewById<AppCompatTextView>(R.id.infoSeekbarDescription) }

    var progress: Int
        get() = seekBar.progress
        set(progress) {
            seekBar.progress = progress
        }

    init {
        inflate(context, R.layout.info_seekbar, this)
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.InfoSeekBar)

        titleTextView.text = attributes.getString(R.styleable.InfoSeekBar_titleText)
        descriptionTextView.text = attributes.getString(R.styleable.InfoSeekBar_descriptionText)
        statusTextView.text = attributes.getString(R.styleable.InfoSeekBar_statusText)

        val attrProgress = attributes.getInt(R.styleable.InfoSeekBar_seekbarProgress, 2)
        seekBar.progress = if (attrProgress < 2) 2 else attrProgress

        attributes.recycle()
    }

    fun setOnSeekBarChangeListener(onSeekBarChangeListener: OnSeekBarChangeListener) {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                onSeekBarChangeListener.onProgressChanged(this@InfoSeekBar, progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                onSeekBarChangeListener.onStartTrackingTouch(this@InfoSeekBar)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                onSeekBarChangeListener.onStopTrackingTouch(this@InfoSeekBar)
            }
        })
    }

    fun setTitleText(title: String) {
        titleTextView.text = title
    }

    fun setDescriptionText(description: String) {
        descriptionTextView.text = description
    }

    fun setStatusText(status: String) {
        statusTextView.text = status
    }
}
