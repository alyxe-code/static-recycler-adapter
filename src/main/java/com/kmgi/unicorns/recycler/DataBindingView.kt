package com.kmgi.unicorns.recycler

import androidx.annotation.LayoutRes
import androidx.databinding.ViewDataBinding

interface DataBindingView<T : ViewDataBinding> {
    @get:LayoutRes
    val layoutId: Int

    fun getId(): Long = NO_ID

    fun bind(binding: T) = Unit

    /**
     * Only in StaticPagingDataAdapter
     */
    fun bind(binding: T, previous: DataBindingView<T>?, next: DataBindingView<T>?) = Unit

    fun unbind(binding: T) = Unit

    fun onAttachedToWindow(binding: T) = Unit
    fun onDetachedFromWindow(binding: T) = Unit

    companion object {
        internal const val NO_ID = -1L
    }
}