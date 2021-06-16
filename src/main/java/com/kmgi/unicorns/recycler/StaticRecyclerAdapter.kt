package com.kmgi.unicorns.recycler

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class StaticRecyclerAdapter<T : ViewDataBinding>(
    private val lifecycleOwnerProvider: () -> LifecycleOwner?,
    cells: List<DataBindingView<T>>,
) : RecyclerView.Adapter<StaticRecyclerAdapter.ViewHolder<T>>() {

    protected var cells: List<DataBindingView<T>> = cells.toList()
    private val boundCells = mutableMapOf<ViewHolder<T>, DataBindingView<T>>()
    private val visibleCells = mutableMapOf<ViewHolder<T>, DataBindingView<T>>()

    private val onListUpdatedListeners = mutableSetOf<OnListUpdatedCallback<T>>()

    fun onListUpdated(listener: OnListUpdatedCallback<T>) {
        onListUpdatedListeners.add(listener)
    }

    fun removeOnListUpdatedListener(listener: OnListUpdatedCallback<T>) {
        onListUpdatedListeners.remove(listener)
    }

    /**
     * Replace with new list and notify recycler that data set changed
     */
    @Suppress("UNCHECKED_CAST")
    @Deprecated("deprecated", replaceWith = ReplaceWith("updateList(newList)"))
    fun updateListHard(newList: List<DataBindingView<*>>) {
        this.cells = newList as List<DataBindingView<T>>
        notifyDataSetChanged()

        onListUpdatedListeners.forEach { it.onUpdated(newList) }
    }

    /**
     * Update list using diff util
     */
    @Suppress("UNCHECKED_CAST")
    fun updateList(newList: List<DataBindingView<*>>, detectMoves: Boolean = true) {
        val oldList = cells.toList()
        DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = oldList.size

                override fun getNewListSize(): Int = newList.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val old = cells[oldItemPosition]
                            as? ComparableDataBindingView<T>
                        ?: return false

                    val new = newList[newItemPosition]
                            as? ComparableDataBindingView<T>
                        ?: return false

                    return old.areItemsTheSame(new)
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int,
                ): Boolean {
                    val old = cells[oldItemPosition]
                            as? ComparableDataBindingView<T>
                        ?: return false

                    val new = newList[newItemPosition]
                            as? ComparableDataBindingView<T>
                        ?: return false

                    return areItemsTheSame(
                        oldItemPosition,
                        newItemPosition
                    ) && old.areContentsTheSame(new)
                }
            },
            detectMoves
        ).let {
            GlobalScope.launch(Dispatchers.Main.immediate) {
                it.dispatchUpdatesTo(this@StaticRecyclerAdapter)

                cells = newList as List<DataBindingView<T>>

                onListUpdatedListeners.forEach { it.onUpdated(newList) }
            }
        }
    }

    override fun getItemCount(): Int = cells.size

    override fun onBindViewHolder(holder: ViewHolder<T>, position: Int) {
        holder.binding ?: return

        val cell = cells[position]
        cell.bind(holder.binding)
        boundCells[holder] = cell
    }

    override fun onViewRecycled(holder: ViewHolder<T>) {
        super.onViewRecycled(holder)

        holder.binding ?: return

        val cell = boundCells.remove(holder)
        if (cell != null) {
            holder.binding.unbind()
            cell.unbind(holder.binding)
        }
    }

    override fun onViewAttachedToWindow(holder: ViewHolder<T>) {
        super.onViewAttachedToWindow(holder)

        holder.binding ?: return

        holder.binding.lifecycleOwner = lifecycleOwnerProvider()
        boundCells[holder]?.onAttachedToWindow(holder.binding)
        visibleCells[holder] = boundCells[holder] as DataBindingView<T>
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder<T>) {
        super.onViewDetachedFromWindow(holder)

        holder.binding ?: return

        visibleCells.remove(holder)
        boundCells[holder]?.onDetachedFromWindow(holder.binding)
        holder.binding.lifecycleOwner = null
    }

    override fun getItemViewType(position: Int): Int = cells[position].layoutId

    override fun onCreateViewHolder(viewGroup: ViewGroup, index: Int): ViewHolder<T> {
        if (index == -1)
            return ViewHolder(null, viewGroup.context)

        val binding: T = DataBindingUtil.inflate(
            LayoutInflater.from(viewGroup.context),
            index,
            viewGroup,
            false
        )
        return ViewHolder(binding, viewGroup.context)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        clearAll()
    }

    private fun clearAll() {
        boundCells.forEach { (holder, binding) ->
            holder.binding ?: return@forEach
            binding.unbind(holder.binding)
            holder.binding.lifecycleOwner = null
        }
        boundCells.clear()
    }

    override fun getItemId(position: Int) = cells[position].getId()
        .takeIf { it > DataBindingView.NO_ID }
        ?: super.getItemId(position)

    fun getViewHolderOrNull(binding: DataBindingView<T>) = boundCells
        .keys
        .firstOrNull { boundCells[it] == binding }

    fun getItemAt(position: Int): DataBindingView<*>? = cells.getOrNull(position)

    val visibleViewHolders: List<ViewHolder<T>>
        get() = visibleCells.map { it.key }

    val visibleDataBindings: List<DataBindingView<T>>
        get() = visibleCells.map { it.value }

    val visibleItems: Map<ViewHolder<T>, DataBindingView<T>>
        get() = visibleCells.toMap()

    class ViewHolder<T : ViewDataBinding>(
        val binding: T?,
        context: Context,
    ) : RecyclerView.ViewHolder(binding?.root ?: View(context))

    fun interface OnListUpdatedCallback<T : ViewDataBinding> {
        fun onUpdated(newList: List<DataBindingView<T>>)
    }
}
