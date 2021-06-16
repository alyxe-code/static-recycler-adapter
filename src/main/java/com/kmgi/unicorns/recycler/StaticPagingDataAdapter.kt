package com.kmgi.unicorns.recycler

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class StaticPagingDataAdapter<T : ViewDataBinding>(
    private val lifecycleOwner: LifecycleOwner,
) : PagingDataAdapter<
        ComparableDataBindingView<T>,
        StaticPagingDataAdapter.ViewHolder<T>
        >(DiffCallback<T>()) {

    private val visibleItems = mutableMapOf<ViewHolder<T>, ComparableDataBindingView<T>>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<T> {
        if (viewType == -1)
            return ViewHolder(null, parent.context)

        val binding = DataBindingUtil.inflate<T>(
            LayoutInflater.from(parent.context),
            viewType,
            parent,
            false
        )

        return ViewHolder(binding, parent.context)
    }

    override fun onBindViewHolder(holder: ViewHolder<T>, position: Int) {
        val item = getItem(position) ?: return

        holder.binding ?: return

        holder.binding.lifecycleOwner = lifecycleOwner

        item.bind(holder.binding)
        item.bind(
            binding = holder.binding,
            previous = try {
                getItem(position - 1)
            } catch (t: Throwable) {
                null
            },
            next = try {
                getItem(position + 1)
            } catch (t: Throwable) {
                null
            },
        )

        visibleItems[holder] = item
    }

    override fun onViewRecycled(holder: ViewHolder<T>) {
        super.onViewRecycled(holder)

        holder.binding ?: return

        visibleItems[holder]?.unbind(holder.binding)

        holder.binding.unbind()
        holder.binding.lifecycleOwner = null
    }

    override fun getItemViewType(position: Int): Int = getItem(position)?.layoutId ?: -1

    class ViewHolder<T : ViewDataBinding>(
        val binding: T?,
        context: Context,
    ) : RecyclerView.ViewHolder(binding?.root ?: View(context))

    class DiffCallback<T : ViewDataBinding> :
        DiffUtil.ItemCallback<ComparableDataBindingView<T>>() {

        override fun areItemsTheSame(
            oldItem: ComparableDataBindingView<T>,
            newItem: ComparableDataBindingView<T>,
        ): Boolean = newItem.areItemsTheSame(oldItem)

        override fun areContentsTheSame(
            oldItem: ComparableDataBindingView<T>,
            newItem: ComparableDataBindingView<T>,
        ): Boolean = newItem.areContentsTheSame(oldItem)
    }
}