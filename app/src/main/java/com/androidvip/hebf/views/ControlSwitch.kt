package com.androidvip.hebf.views

import android.content.Context
import android.util.AttributeSet
import android.widget.CompoundButton
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import com.androidvip.hebf.R
import com.androidvip.hebf.runOnMainThread
import com.google.android.material.switchmaterial.SwitchMaterial
import com.topjohnwu.superuser.ShellUtils

class ControlSwitch(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    private val controlSwitch: SwitchMaterial by lazy {
        findViewById<SwitchMaterial>(R.id.controlSwitch)
    }
    private val titleTextView: AppCompatTextView by lazy {
        findViewById<AppCompatTextView>(R.id.controlSwitchTitle)
    }
    private val descriptionTextView: AppCompatTextView by lazy {
        findViewById<AppCompatTextView>(R.id.controlSwitchDescription)
    }
    private val layout: FrameLayout by lazy { findViewById<FrameLayout>(R.id.controlSwitchLayout) }

    init {
        inflate(context, R.layout.control_switch, this)
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ControlSwitch)

        titleTextView.text = attributes.getString(R.styleable.ControlSwitch_title)
        descriptionTextView.text = attributes.getString(R.styleable.ControlSwitch_description)

        layout.setOnClickListener {
            controlSwitch.isChecked = !controlSwitch.isChecked
        }

        attributes.recycle()
    }

    fun setTitleText(title: String) {
        titleTextView.text = title
    }

    fun setDescriptionText(description: String) {
        descriptionTextView.text = description
    }

    fun setOnCheckedChangeListener(listener: CompoundButton.OnCheckedChangeListener?) {
        controlSwitch.setOnCheckedChangeListener(listener)
    }

    fun setChecked(checked: Boolean) {
        if (ShellUtils.onMainThread()) {
            controlSwitch.isChecked = checked
        } else {
            controlSwitch.context.runOnMainThread { controlSwitch.isChecked = checked }
        }
    }

    fun setControlEnabled(enabled: Boolean) {
        if (ShellUtils.onMainThread()) {
            controlSwitch.isEnabled = enabled
        } else {
            controlSwitch.context.runOnMainThread { controlSwitch.isEnabled = enabled }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        setControlEnabled(enabled)
    }

    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
        layout.setOnClickListener(l)
    }
}
