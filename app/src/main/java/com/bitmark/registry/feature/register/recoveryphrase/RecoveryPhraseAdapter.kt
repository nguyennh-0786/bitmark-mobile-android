package com.bitmark.registry.feature.register.recoveryphrase

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import kotlinx.android.synthetic.main.item_recovery_phrase.view.*


/**
 * @author Hieu Pham
 * @since 2019-07-08
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RecoveryPhraseAdapter(
    private val editable: Boolean = true,
    @ColorRes private val textColor: Int = R.color.gray
) : RecyclerView.Adapter<RecoveryPhraseAdapter.ViewHolder>(),
    OnTextChangeListener {

    private val items = mutableListOf<Item>()

    private var onTextChangeListener: OnTextChangeListener? = null

    private var onDoneListener: (() -> Unit)? = null

    fun setOnTextChangeListener(listener: OnTextChangeListener?) {
        this.onTextChangeListener = listener
    }

    fun setOnDoneListener(listener: () -> Unit) {
        this.onDoneListener = listener
    }

    internal fun requestNextFocus(): Boolean {
        val focusedIndex = items.indexOfFirst { i -> i.focused }
        if (focusedIndex != -1) {
            items[focusedIndex].focused = false
            notifyItemChanged(focusedIndex)
            val nextFocusIndex =
                items.indexOfFirst { i -> i.sequence == items[focusedIndex].sequence + 1 }
            if (nextFocusIndex != -1) {
                items[nextFocusIndex].focused = true
                notifyItemChanged(nextFocusIndex)
                return true
            }
        }
        return false
    }

    internal fun requestPrevFocus(): Boolean {
        val focusedIndex = items.indexOfFirst { i -> i.focused }
        if (focusedIndex != -1) {
            items[focusedIndex].focused = false
            notifyItemChanged(focusedIndex)
            val prevFocusIndex =
                items.indexOfFirst { i -> i.sequence == items[focusedIndex].sequence - 1 }
            if (prevFocusIndex != -1) {
                items[prevFocusIndex].focused = true
                notifyItemChanged(prevFocusIndex)
                return true
            }
        }
        return false
    }

    fun setDefault(version: Version = Version.TWELVE) {
        val length = version.value
        if (length % 2 != 0) throw RuntimeException("must be even number")
        val loop = length / 2
        items.clear()
        for (sequence in 1..loop) {
            items.add(Item(sequence, "", textColor = textColor))
            items.add(Item(sequence + loop, "", textColor = textColor))
        }
        notifyDataSetChanged()
    }

    fun set(words: Array<String>) {
        set(words, null)
    }

    fun set(words: Array<String>, hiddenValues: Array<String>? = null) {
        if (words.size % 2 != 0) throw RuntimeException("must be even number")
        items.clear()
        val loop = words.size / 2
        for (index in 0 until loop) {
            val sequence1 = index + 1
            val sequence2 = sequence1 + loop
            val word1 = words[sequence1 - 1]
            val word2 = words[sequence2 - 1]

            items.add(
                Item(
                    sequence1,
                    word1,
                    hiddenValues?.contains(word1) ?: false,
                    textColor = textColor
                )
            )
            items.add(
                Item(
                    sequence2,
                    word2,
                    hiddenValues?.contains(word2) ?: false,
                    textColor = textColor
                )
            )
        }
        notifyDataSetChanged()
    }

    fun set(word: String) {
        val focusedIndex = items.indexOfFirst { i -> i.focused }
        if (focusedIndex == -1) return
        items[focusedIndex].word = word
        notifyItemChanged(focusedIndex)
    }

    fun showHiddenSequentially(word: String) {
        val items = ArrayList<Item>(this.items)
        items.sortWith(Comparator { o1, o2 -> o1.sequence.compareTo(o2.sequence) })
        val item = items.find { i -> i.hidden }

        if (item != null) {
            item.hidden = false
            item.word = word
            item.textColor = R.color.blue_ribbon
            val pos = this.items.indexOf(item)
            notifyItemChanged(pos)
        }
    }

    fun isItemsVisible() = this.items.find { i -> i.hidden } == null

    fun compare(words: Array<String>): Boolean {
        if (words.size != items.size) return false
        for (index in 0 until words.size) {
            val item = items.find { i -> i.word == words[index] }
            if (item == null || item.sequence != index + 1) return false
        }
        return true
    }

    fun setColors(@ColorRes color: Int) {
        this.items.forEach { i -> i.textColor = color }
        notifyDataSetChanged()
    }

    fun getPhrase(): Array<String?> {
        val phrase = arrayOfNulls<String>(items.size)
        for (item in items) {
            phrase[item.sequence - 1] = item.word
        }
        return phrase
    }

    fun isValid() = null == items.find { it.word.isEmpty() }

    override fun afterTextChanged(item: Item) {
        items.find { it.sequence == item.sequence }
            ?.also { it.word = item.word }
        onTextChangeListener?.afterTextChanged(item)
    }

    override fun onTextChanged(item: Item) {
        onTextChangeListener?.onTextChanged(item)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recovery_phrase, parent, false)
        return ViewHolder(view, editable, this)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(
        view: View,
        editable: Boolean,
        private val listener: OnTextChangeListener
    ) :
        RecyclerView.ViewHolder(view) {

        private lateinit var item: Item

        init {
            with(itemView) {
                if (!editable) {
                    edtWord.isFocusable = false
                }

                edtWord.doOnTextChanged { text, _, _, _ ->
                    item.word = text.toString()

                    edtWord.background = ContextCompat.getDrawable(
                        context,
                        if (text.isNullOrBlank()) R.drawable.bg_border_blue_ribbon_wild_sand_stateful else R.drawable.bg_border_blue_ribbon_white_stateful
                    )

                    listener.onTextChanged(item)
                }

                edtWord.doAfterTextChanged {
                    listener.afterTextChanged(item)
                }

                edtWord.setOnFocusChangeListener { _, hasFocus ->
                    item.focused = hasFocus
                    val text = edtWord.text
                    if (!hasFocus && !text.isNullOrBlank()) {
                        edtWord.background = null
                    } else {
                        edtWord.background = ContextCompat.getDrawable(
                            context,
                            if (text.isNullOrBlank()) R.drawable.bg_border_blue_ribbon_wild_sand_stateful else R.drawable.bg_border_blue_ribbon_white_stateful
                        )
                    }
                }

                edtWord.setOnEditorActionListener { _, actionId, event ->
                    if (event == null) {
                        when (actionId) {
                            EditorInfo.IME_ACTION_NEXT -> {
                                requestNextFocus()
                                true
                            }

                            EditorInfo.IME_ACTION_DONE -> {
                                edtWord.clearFocus()
                                onDoneListener?.invoke()
                                true
                            }

                            else -> false
                        }
                    } else false
                }
            }
        }

        fun bind(item: Item) {
            this.item = item
            with(itemView) {
                tvNo.text = "%d.".format(item.sequence)
                edtWord.setText(if (item.hidden) "" else item.word)
                edtWord.setTextColor(
                    ContextCompat.getColor(
                        context,
                        item.textColor
                    )
                )

                edtWord.imeOptions =
                    if (item.sequence == items.size) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT

                if (item.focused) {
                    edtWord.requestFocus()
                } else {
                    edtWord.clearFocus()
                }

            }

        }
    }
}

interface OnTextChangeListener {

    fun onTextChanged(item: Item)

    fun afterTextChanged(item: Item)
}

class Item(
    internal val sequence: Int,
    internal var word: String,
    internal var hidden: Boolean = false,
    internal var textColor: Int = R.color.gray,
    internal var focused: Boolean = false
)

enum class Version(val value: Int) {
    TWELVE(12), TWENTY_FOUR(24)
}