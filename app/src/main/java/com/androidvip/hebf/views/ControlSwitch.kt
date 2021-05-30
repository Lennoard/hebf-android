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
import com.google.android.material.switchmaterial.SwitchMaterial
import com.topjohnwu.superuser.ShellUtils

class ControlSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    private val controlSwitch: SwitchMaterial by lazy {
        findViewById(R.id.controlSwitch)
    }

    init {
        inflate(context, R.layout.control_switch, this)
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ControlSwitch)
        val title = attributes.getString(R.styleable.ControlSwitch_title)
        val enabled = attributes.getBoolean(R.styleable.ControlSwitch_android_enabled, true)
        val checked = attributes.getBoolean(R.styleable.ControlSwitch_android_checked, false)
        val description = attributes.getString(R.styleable.ControlSwitch_description)
        val infoButton = findViewById<AppCompatImageView>(R.id.infoButton)

        infoButton.setOnClickListener {
            getFragmentManager(context)?.let {
                ModalBottomSheet.newInstance(title, description).show(it, "ControlSwitch")
            }
        }

        controlSwitch.text = title
        setChecked(checked)
        isEnabled = enabled
        attributes.recycle()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        setControlEnabled(enabled)
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

    private fun setControlEnabled(enabled: Boolean) {
        if (ShellUtils.onMainThread()) {
            controlSwitch.isEnabled = enabled
        } else {
            controlSwitch.context.runOnMainThread { controlSwitch.isEnabled = enabled }
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
