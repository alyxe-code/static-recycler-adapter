package com.kmgi.unicorns.recycler

import androidx.databinding.ViewDataBinding

interface ComparableDataBindingView<T : ViewDataBinding> : DataBindingView<T> {
    fun areItemsTheSame(old: ComparableDataBindingView<T>): Boolean
    fun areContentsTheSame(old: ComparableDataBindingView<T>): Boolean
}