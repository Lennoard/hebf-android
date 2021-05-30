package com.androidvip.hebf.views

import android.content.Context
import android.util.AttributeSet
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.androidvip.hebf.R
import com.androidvip.hebf.runOnMainThread
import com.androidvip.hebf.utils.ModalBottomSheet
import com.google.android.material.checkbox.MaterialCheckBox
import com.topjohnwu.superuser.ShellUtils

class ControlCheckBox @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    private val controlCheckBox: MaterialCheckBox by lazy {
        findViewById(R.id.controlCheckBox)
    }

    init {
        inflate(context, R.layout.control_checkbox, this)
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ControlCheckBox)
        val title = attributes.getString(R.styleable.ControlCheckBox_android_text)
        val enabled = attributes.getBoolean(R.styleable.ControlCheckBox_android_enabled, true)
        val checked = attributes.getBoolean(R.styleable.ControlCheckBox_android_checked, false)
        val description = attributes.getString(R.styleable.ControlCheckBox_controlDescription)
        val infoButton = findViewById<AppCompatImageView>(R.id.infoButton)

        infoButton.setOnClickListener {
            getFragmentManager(context)?.let {
                ModalBottomSheet.newInstance(title, description).show(it, "ControlCheckBox")
            }
        }

        controlCheckBox.text = title
        setChecked(checked)
        isEnabled = enabled
        attributes.recycle()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        setControlEnabled(enabled)
    }

    fun setOnCheckedChangeListener(listener: CompoundButton.OnCheckedChangeListener?) {
        controlCheckBox.setOnCheckedChangeListener(listener)
    }

    fun setChecked(checked: Boolean) {
        if (ShellUtils.onMainThread()) {
            controlCheckBox.isChecked = checked
        } else {
            controlCheckBox.context.runOnMainThread { controlCheckBox.isChecked = checked }
        }
    }

    private fun setControlEnabled(enabled: Boolean) {
        if (ShellUtils.onMainThread()) {
            controlCheckBox.isEnabled = enabled
        } else {
            controlCheckBox.context.runOnMainThread { controlCheckBox.isEnabled = enabled }
        }
    }

    private fun getFragmentManager(context: Context): FragmentManager? {
        return when (context) {
            is AppCompatActivity -> context.supportFragmentManager
            is FragmentActivity -> context.supportFragmentManager
            is ContextThemeWrapper -> getFragmentManager(context.baseContext)
            is android.view.ContextThemeWrapper -> getFragmentManager(context.baseContext)
            else -> null
        }
    }
}
