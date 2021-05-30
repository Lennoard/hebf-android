package com.androidvip.hebf.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import com.androidvip.hebf.R
import com.androidvip.hebf.goAway
import com.androidvip.hebf.show
import com.androidvip.hebf.utils.Utils
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.card.MaterialCardView

class DashCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private val titleTextView: AppCompatTextView by lazy { findViewById(R.id.dashCardTitle) }
    private val valueTextView: AppCompatTextView by lazy { findViewById(R.id.dashCardValue) }
    private val subTextView: AppCompatTextView by lazy { findViewById(R.id.dashCardSubText) }
    private val card: MaterialCardView by lazy { findViewById(R.id.dashCard) }
    private val chart: LineChart by lazy { findViewById(R.id.dashCardChart) }
    private val rootLayout: LinearLayout by lazy { findViewById(R.id.dashCardLayout) }

    init {
        inflate(context, R.layout.dash_card, this)
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.DashCard)

        titleTextView.text = attributes.getString(R.styleable.DashCard_cardTitle)

        attributes.getString(R.styleable.DashCard_cardValue)?.let {
            valueTextView.text = it
            valueTextView.show()
        }

        attributes.getString(R.styleable.DashCard_cardSubText)?.let {
            subTextView.text = it
            subTextView.show()
        }

        attributes.recycle()

        with (chart) {
            description.isEnabled = false
            isDragEnabled = false
            setTouchEnabled(false)
            setScaleEnabled(false)
            setDrawMarkers(false)
            axisRight.isEnabled = false
            axisLeft.isEnabled = false
            legend.isEnabled = false
        }

        with (chart.xAxis) {
            setDrawGridLines(false)
            setDrawAxisLine(false)
            setDrawLabels(false)
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        rootLayout.setOnClickListener(l)
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        rootLayout.setOnLongClickListener(l)
    }

    override fun addView(view: View) {
        rootLayout.addView(view)
    }

    override fun removeView(view: View) {
        rootLayout.removeView(view)
    }

    fun addCard(dashCard: DashCard) {
        val separator = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    Utils.dpToPx(context, 1)
            ).apply {
                setMargins(0, Utils.dpToPx(context, 16), 0, Utils.dpToPx(context, 16))
            }
            setBackgroundColor(Color.parseColor("#CCCCCC"))
        }

        val rootLayout = dashCard.rootLayout.apply {
            setPadding(0, 0, 0, 0)
        }
        dashCard.card.removeView(rootLayout)
        addView(separator)
        addView(rootLayout)
    }


    fun setTitleText(title: String) {
        titleTextView.text = title
    }

    fun setValueText(value: String) {
        valueTextView.text = value
        valueTextView.show()
    }

    fun setValueTextColor(@ColorInt color: Int) {
        valueTextView.setTextColor(color)
    }

    fun setSubText(text: String) {
        subTextView.text = text
        subTextView.show()
    }

    private fun setChartData(data: LineData?) {
        if (data == null) {
            chart.goAway()
        } else {
            chart.data = data
            chart.show()
        }
    }

    fun setChartDataSet(dataSet: LineDataSet?) {
        when {
            dataSet == null -> {
                chart.goAway()
            }
            chart.data != null -> {
                chart.data = LineData(dataSet)
                chart.data.notifyDataChanged()
                chart.notifyDataSetChanged()
                chart.show()
            }
            else -> {
                setChartData(LineData(dataSet))
            }
        }
    }
}
