package com.androidvip.hebf.utils

import android.app.Activity
import android.content.DialogInterface
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.androidvip.hebf.R
import com.androidvip.hebf.show
import com.androidvip.hebf.toPx
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.context_bottom_sheet.*
import kotlinx.android.synthetic.main.modal_bottom_sheet.*
import java.io.Serializable

typealias SheetOption = ContextBottomSheet.Option

class ContextBottomSheet : BottomSheetDialogFragment() {
    companion object {
        fun newInstance(title: String? = "", options: ArrayList<Option>): ContextBottomSheet = ContextBottomSheet().apply {
            arguments = Bundle().apply {
                putString("title", title)
                putSerializable("options", options)
            }
        }
    }

    interface OnOptionClickListener {
        fun onOptionClick(tag: String)
    }

    data class Option(var title: String, val tag: String, @DrawableRes var icon: Int?) : Serializable

    var onOptionClickListener: OnOptionClickListener = object : OnOptionClickListener {
        override fun onOptionClick(tag: String) {
            dismiss()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.context_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            val title = it.getString("title", "")
            if (title.isNotEmpty()) {
                contextBottomSheetTitle.show()
                contextBottomSheetTitle.text = title
            }

            val options = it.getSerializable("options") as ArrayList<*>?
            options?.forEach { option ->
                (option as Option?)?.let { opt ->
                    contextBottomSheetOptionsContainer.addView(generateOptionView(opt))
                }
            }
        }
    }

    private fun generateOptionView(option: Option): LinearLayout {
        val textColor = TypedValue()
        val backgroundDrawable = TypedValue()
        context?.theme?.resolveAttribute(R.attr.colorOnSurface, textColor, true)
        context?.theme?.resolveAttribute(R.attr.selectableItemBackground, backgroundDrawable, true)

        val rootLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                48.toPx(context)
            )
            gravity = Gravity.CENTER_VERTICAL
            tag = option.tag
            setPadding(16.toPx(context), 0, 16.toPx(context), 0)
            setBackgroundResource(backgroundDrawable.resourceId)
            setOnClickListener {
                onOptionClickListener.onOptionClick(option.tag)
            }
        }

        option.icon?.let {
            rootLayout.addView(ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(24.toPx(context), 24.toPx(context)).apply {
                    setMargins(0, 0, 16.toPx(context), 0)
                }
                setImageResource(it)

                if (option.tag == "uninstall") {
                    setColorFilter(ContextCompat.getColor(context, R.color.colorError), PorterDuff.Mode.SRC_IN)
                } else {
                    setColorFilter(ContextCompat.getColor(context, textColor.resourceId), PorterDuff.Mode.SRC_IN)
                }
            })
        }

        rootLayout.addView(TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1F)
            text = option.title
            if (option.tag == "uninstall") {
                setTextColor(ContextCompat.getColor(context, R.color.colorError))
            } else {
                setTextColor(ContextCompat.getColor(context, textColor.resourceId))
            }
        })

        return rootLayout
    }
}

class ModalBottomSheet : BottomSheetDialogFragment() {

    interface OnButtonClickListener {
        fun onOkButtonClick()
        fun onCancelButtonClick()
    }

    var okButtonText: String? = ""
    var cancelButtonText: String? = ""
    var showOkButton = true
    var showCancelButton = false
    var onButtonClickListener: OnButtonClickListener = object : OnButtonClickListener {
        override fun onOkButtonClick() {
            dismiss()
        }

        override fun onCancelButtonClick() {
            dismiss()
        }
    }

    companion object {
        fun newInstance(title: String? = "", message: String? = ""): ModalBottomSheet = ModalBottomSheet().apply {
            arguments = Bundle().apply {
                putString("title", title)
                putString("message", message)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.modal_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            modalBottomSheetTitle.text = it.getString("title", "")
            modalBottomSheetMessage.text = it.getString("message", "")
        }

        okButtonText?.let {
            if (it.isNotEmpty()) {
                modalBottomSheetOk.text = okButtonText
            }
        }

        cancelButtonText?.let {
            if (it.isNotEmpty()) {
                modalBottomSheetCancel.text = cancelButtonText
            }
        }

        modalBottomSheetOk.setOnClickListener { onButtonClickListener.onOkButtonClick() }
        modalBottomSheetCancel.setOnClickListener { onButtonClickListener.onCancelButtonClick() }

        modalBottomSheetOk.visibility = if (showOkButton) View.VISIBLE else View.GONE
        modalBottomSheetCancel.visibility = if (showCancelButton) View.VISIBLE else View.GONE
    }
}

/**
 * An useful class that displays a dialog with a single input field and get the text of it.
 * It is also possible to set the input type, and add a cancelListener
 *
 * @author Lennoard
 * @see AlertDialog
 */
class EditDialog(private val activity: Activity) {
    var title = "Edit"
    var message: String? = null
    var inputText: String? = ""
    var inputHint: String? = ""
    var inputType = INPUT_TYPE_UNSPECIFIED
    var guessInputType = false
    var onCancelListener = DialogInterface.OnClickListener { _, _ -> }
    var onConfirmListener: OnConfirmListener = object : OnConfirmListener {
        override fun onOkButtonClicked(newData: String) {

        }
    }

    private companion object {
        private const val INPUT_TYPE_UNSPECIFIED = -1
    }

    /**
     * Interface for when the positive button is clicked,
     * giving the input field text as "newData"
     */
    interface OnConfirmListener {
        fun onOkButtonClicked(newData: String)
    }

    fun setTitle(@StringRes title: Int) {
        this.title = activity.getString(title)
    }

    fun setInputHint(@StringRes hint: Int) {
        this.inputHint = activity.getString(hint)
    }

    fun show() {
        if (activity.isFinishing) return

        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_input, null)

        val editText = dialogView?.findViewById<EditText>(R.id.dialogTextInput)
        editText?.setText(if (inputText == null) "" else inputText)
        editText?.hint = if (inputHint == null) "" else inputHint
        editText?.inputType = when {
            guessInputType -> guessInputType()
            inputType == INPUT_TYPE_UNSPECIFIED -> InputType.TYPE_CLASS_TEXT
            else -> inputType
        }

        val dialog = AlertDialog.Builder(activity).apply {
            setTitle(title)
            setView(dialogView)
            this@EditDialog.message?.let { setMessage(it) }

            setNegativeButton(android.R.string.cancel, onCancelListener)
            setPositiveButton(android.R.string.ok) { _, _ -> onConfirmListener.onOkButtonClicked(editText?.text.toString()) }
        }


        dialog.show()
    }

    fun buildApplying(block: EditDialog.() -> Unit): EditDialog {
        this.block()
        return this
    }

    private fun guessInputType(): Int {
        if (inputText == null) return InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL

        return try {
            inputText!!.toInt()
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_NORMAL
        } catch (e: Exception) {
            try {
                inputText!!.toDouble()
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            } catch (e: Exception) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
            }
        }
    }
}