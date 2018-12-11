package org.tokend.authenticator.view

import android.content.Context
import android.support.annotation.StringRes
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.tokend.authenticator.R
import org.tokend.authenticator.util.errorhandler.ErrorHandler

class ErrorEmptyView : LinearLayout {
    constructor(context: Context, attributeSet: AttributeSet?) :
            super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet?, style: Int) :
            super(context, attributeSet, style)

    constructor(context: Context) : super(context)

    private val messageTextView: TextView
    private val actionButton: Button
    private val buttonHolder: LinearLayout

    private var emptyViewDenial: () -> Boolean = { false }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        visibility = View.GONE

        LayoutInflater.from(context).inflate(R.layout.layout_error_empty_view,
                this, true)

        messageTextView = findViewById(R.id.message_text_view)
        buttonHolder = findViewById(R.id.button_holder)
        actionButton = findViewById(R.id.action_button)
    }

    fun hide() {
        visibility = View.GONE
    }

    fun showEmpty(@StringRes messageId: Int) {
        showEmpty(context.getString(messageId))
    }

    fun showEmpty(message: String) {
        visibility = View.VISIBLE

        messageTextView.text = message
        actionButton.visibility = View.GONE
        buttonHolder.visibility = View.GONE
    }

    fun showError(throwable: Throwable, errorHandler: ErrorHandler,
                  actionButtonClick: (() -> Unit)? = null) {
        showError(errorHandler.getErrorMessage(throwable),
                actionButtonClick)
    }

    fun showError(error: String?, actionButtonClick: (() -> Unit)? = null) {
        error ?: return

        visibility = View.VISIBLE

        messageTextView.text = error

        if (actionButtonClick != null) {
            buttonHolder.visibility = View.VISIBLE
            actionButton.visibility = View.VISIBLE
            actionButton.setOnClickListener { actionButtonClick.invoke() }
        } else {
            actionButton.visibility = View.GONE
            buttonHolder.visibility = View.GONE
        }
    }

    fun observeAdapter(adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
                       @StringRes messageId: Int) {
        adapter.registerAdapterDataObserver(getEmptyObserver(adapter) {
            context.getString(messageId)
        })
    }

    fun observeAdapter(adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
                       messageProvider: () -> String) {
        adapter.registerAdapterDataObserver(getEmptyObserver(adapter, messageProvider))
    }

    fun setEmptyViewDenial(denial: () -> Boolean) {
        this.emptyViewDenial = denial
    }

    private fun getEmptyObserver(adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
                                 messageProvider: () -> String):
            RecyclerView.AdapterDataObserver {
        return object : RecyclerView.AdapterDataObserver() {
            private fun operate() {
                if (adapter.itemCount > 0) {
                    hide()
                } else {
                    if (!emptyViewDenial()) {
                        showEmpty(messageProvider())
                    } else {
                        hide()
                    }
                }
            }

            override fun onChanged() {
                operate()
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                operate()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                operate()
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                operate()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                operate()
            }
        }
    }
}