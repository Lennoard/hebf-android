package com.androidvip.hebf.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.core.widget.ImageViewCompat
import com.androidvip.hebf.R
import com.androidvip.hebf.runOnMainThread
import com.google.android.material.switchmaterial.SwitchMaterial
import com.topjohnwu.superuser.ShellUtils

class ServiceState @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : FrameLayout(context, attrs, defStyle) {

    private val switch: SwitchMaterial by lazy {
        findViewById(R.id.stateSwitch)
    }
    private val titleTextView: AppCompatTextView by lazy {
        findViewById(R.id.stateTitle)
    }
    private val statusTextView: AppCompatTextView by lazy {
        findViewById(R.id.stateCaption)
    }
    private val icon: AppCompatImageView by lazy {
        findViewById(R.id.stateIcon)
    }

    private val layout: ConstraintLayout by lazy { findViewById(R.id.stateLayout) }

    init {
        inflate(context, R.layout.service_state, this)
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ServiceState)

        try {
            titleTextView.text = attributes.getString(R.styleable.ServiceState_android_text)
            setChecked(attributes.getBoolean(R.styleable.ServiceState_android_checked, false))
            isEnabled = attributes.getBoolean(R.styleable.ServiceState_android_enabled, true)
            layout.setOnClickListener {
                if (isEnabled) {
                    switch.isChecked = !switch.isChecked
                }
            }

            val icon = attributes.getDrawable(R.styleable.ServiceState_serviceIcon)
            this.icon.setImageDrawable(icon)
        } finally {
            attributes.recycle()
        }
    }

    fun setTitleText(title: String) {
        titleTextView.text = title
    }

    fun setOnCheckedChangeListener(f: (Boolean) -> Unit) {
        switch.setOnCheckedChangeListener { _, isChecked ->
            statusTextView.text = if (isChecked) "ON" else "OFF"
            f.invoke(isChecked)
        }
    }

    fun setChecked(checked: Boolean) {
        if (ShellUtils.onMainThread()) {
            switch.isChecked = checked
        } else {
            switch.context.runOnMainThread { switch.isChecked = checked }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        val block = {
            layout.forEach { it.isEnabled = enabled }
            val iconColor = if (enabled) {
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorWarning))
            } else {
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.disabled))
            }

            val textColor = if (enabled) {
                ContextCompat.getColor(context, R.color.colorOnSurface)
            } else {
                ContextCompat.getColor(context, R.color.disabled)
            }
            titleTextView.setTextColor(textColor)
            ImageViewCompat.setImageTintList(icon, iconColor)
        }

        if (ShellUtils.onMainThread()) {
            block()
        } else {
            switch.context.runOnMainThread {
               block()
            }
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
        layout.setOnClickListener(l)
    }
}
