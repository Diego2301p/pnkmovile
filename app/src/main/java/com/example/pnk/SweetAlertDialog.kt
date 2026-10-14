package com.example.pnk

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Custom SweetAlert-style dialog that provides visual feedback
 * similar to the SweetAlert library using Material components.
 */
class SweetAlertDialog(private val context: Context, private val type: Int) {

    companion object {
        const val SUCCESS_TYPE = 1
        const val ERROR_TYPE = 2
        const val WARNING_TYPE = 3
        const val NORMAL_TYPE = 0
    }

    private var titleText: String = ""
    private var contentText: String = ""
    private var confirmText: String = "OK"
    private var cancelText: String = "Cancelar"
    private var onConfirm: ((SweetAlertDialog) -> Unit)? = null
    private var onCancel: ((SweetAlertDialog) -> Unit)? = null
    private var showCancel: Boolean = false
    private var dialog: AlertDialog? = null

    fun setTitleText(title: String): SweetAlertDialog {
        this.titleText = title
        return this
    }

    fun setContentText(content: String): SweetAlertDialog {
        this.contentText = content
        return this
    }

    fun setConfirmText(text: String): SweetAlertDialog {
        this.confirmText = text
        return this
    }

    fun setCancelText(text: String): SweetAlertDialog {
        this.cancelText = text
        this.showCancel = true
        return this
    }

    fun setConfirmClickListener(listener: (SweetAlertDialog) -> Unit): SweetAlertDialog {
        this.onConfirm = listener
        return this
    }

    fun setCancelClickListener(listener: (SweetAlertDialog) -> Unit): SweetAlertDialog {
        this.onCancel = listener
        this.showCancel = true
        return this
    }

    fun dismissWithAnimation() {
        dialog?.dismiss()
    }

    fun show(): SweetAlertDialog {
        val layout = createLayout()
        val builder = AlertDialog.Builder(context)
            .setView(layout)
            .setCancelable(true)
        dialog = builder.create()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.show()
        return this
    }

    private fun createLayout(): View {
        val density = context.resources.displayMetrics.density

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((24 * density).toInt(), (28 * density).toInt(), (24 * density).toInt(), (20 * density).toInt())
            val bg = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 16 * density
            }
            background = bg
        }

        // Icon circle
        val iconSize = (60 * density).toInt()
        val iconView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER
                bottomMargin = (12 * density).toInt()
            }
            val circleBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(getTypeColor())
            }
            background = circleBg
            setPadding((14 * density).toInt(), (14 * density).toInt(), (14 * density).toInt(), (14 * density).toInt())
            setColorFilter(Color.WHITE)
        }

        // Set icon based on type
        val iconRes = when (type) {
            SUCCESS_TYPE -> android.R.drawable.ic_input_add
            ERROR_TYPE -> android.R.drawable.ic_delete
            WARNING_TYPE -> android.R.drawable.ic_dialog_alert
            else -> android.R.drawable.ic_dialog_info
        }
        iconView.setImageResource(iconRes)
        container.addView(iconView)

        // Title
        if (titleText.isNotEmpty()) {
            val titleView = TextView(context).apply {
                text = titleText
                textSize = 20f
                setTextColor(Color.parseColor("#37474F"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (8 * density).toInt()
                }
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            container.addView(titleView)
        }

        // Content
        if (contentText.isNotEmpty()) {
            val contentView = TextView(context).apply {
                text = contentText
                textSize = 14f
                setTextColor(Color.parseColor("#757575"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (20 * density).toInt()
                }
            }
            container.addView(contentView)
        }

        // Buttons container
        val btnContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        if (showCancel) {
            val cancelBtn = createButton(cancelText, Color.parseColor("#9E9E9E"))
            cancelBtn.setOnClickListener {
                onCancel?.invoke(this) ?: dismissWithAnimation()
            }
            val cancelParams = LinearLayout.LayoutParams(0, (44 * density).toInt(), 1f).apply {
                marginEnd = (8 * density).toInt()
            }
            cancelBtn.layoutParams = cancelParams
            btnContainer.addView(cancelBtn)
        }

        val confirmBtn = createButton(confirmText, getTypeColor())
        confirmBtn.setOnClickListener {
            onConfirm?.invoke(this) ?: dismissWithAnimation()
        }
        val confirmParams = if (showCancel) {
            LinearLayout.LayoutParams(0, (44 * density).toInt(), 1f)
        } else {
            LinearLayout.LayoutParams((160 * density).toInt(), (44 * density).toInt()).apply {
                gravity = Gravity.CENTER
            }
        }
        confirmBtn.layoutParams = confirmParams
        btnContainer.addView(confirmBtn)

        container.addView(btnContainer)
        return container
    }

    private fun createButton(text: String, color: Int): TextView {
        val density = context.resources.displayMetrics.density
        return TextView(context).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val bg = GradientDrawable().apply {
                setColor(color)
                cornerRadius = 8 * density
            }
            background = bg
            setPadding((16 * density).toInt(), (10 * density).toInt(), (16 * density).toInt(), (10 * density).toInt())
        }
    }

    private fun getTypeColor(): Int {
        return when (type) {
            SUCCESS_TYPE -> Color.parseColor("#4CAF50")
            ERROR_TYPE -> Color.parseColor("#D32F2F")
            WARNING_TYPE -> Color.parseColor("#FFC107")
            else -> Color.parseColor("#1976D2")
        }
    }
}
