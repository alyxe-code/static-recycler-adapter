package com.kmgi.unicorns.recycler

import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner

class StaticRecyclerAdapterLazy<T : ViewDataBinding>(
    private val initialValueFactory: () -> List<DataBindingView<T>>,
    private val with: StaticRecyclerAdapter<T>.() -> Unit = {},
    private val lifecycleOwnerProvider: () -> LifecycleOwner?,
) : Lazy<StaticRecyclerAdapter<T>> {
    private var cache: StaticRecyclerAdapter<T>? = null

    override val value: StaticRecyclerAdapter<T>
        get() = cache ?: StaticRecyclerAdapter(
            lifecycleOwnerProvider = lifecycleOwnerProvider,
            cells = initialValueFactory(),
        ).also { cache = it.apply(with) }

    override fun isInitialized() = cache != null
}

fun Fragment.staticRecyclerAdapter(
    initialValue: (() -> List<DataBindingView<ViewDataBinding>>) = { emptyList() },
    with: StaticRecyclerAdapter<ViewDataBinding>.() -> Unit = {},
    lifecycleOwnerFactory: () -> LifecycleOwner = { viewLifecycleOwner },
) = StaticRecyclerAdapterLazy(initialValue, with, lifecycleOwnerFactory)

fun Fragment.staticPagingDataAdapter(
    lifecycleOwnerFactory: () -> LifecycleOwner = { viewLifecycleOwner },
) = lazy {
    val adapter = StaticPagingDataAdapter<ViewDataBinding>(lifecycleOwnerFactory())
    adapter
}