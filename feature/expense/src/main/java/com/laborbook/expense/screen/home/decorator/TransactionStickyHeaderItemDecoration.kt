package com.laborbook.expense.screen.home.decorator

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TransactionStickyHeaderItemDecoration(
    private val getHeaderView: (position: Int) -> View?,
    private val isHeader: (position: Int) -> Boolean
) : RecyclerView.ItemDecoration() {

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val layoutManager = parent.layoutManager as? LinearLayoutManager ?: return

        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
        if (firstVisibleItemPosition == RecyclerView.NO_POSITION) return

        val headerPosition = getStickyHeaderPosition(firstVisibleItemPosition)
        if (headerPosition == RecyclerView.NO_POSITION) return

        val headerView = getHeaderView(headerPosition) ?: return
        val topChild = parent.findViewHolderForAdapterPosition(firstVisibleItemPosition)?.itemView ?: return

        headerView.measure(
            View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.AT_MOST)
        )
        headerView.layout(0, 0, headerView.measuredWidth, headerView.measuredHeight)

        val nextHeaderView = getNextHeaderView(parent, firstVisibleItemPosition)

        // Determine the sticky header's Y position
        val headerOffset = getHeaderOffset(nextHeaderView, topChild, headerView)
        c.save()
        c.translate(0f, headerOffset.toFloat())
        headerView.draw(c)
        c.restore()
    }

    private fun getStickyHeaderPosition(position: Int): Int {
        for (i in position downTo 0) {
            if (isHeader(i)) return i
        }
        return RecyclerView.NO_POSITION
    }

    private fun getNextHeaderView(parent: RecyclerView, firstVisibleItemPosition: Int): View? {
        for (i in firstVisibleItemPosition + 1 until parent.childCount) {
            val nextPosition = parent.getChildAdapterPosition(parent.getChildAt(i))
            if (isHeader(nextPosition)) {
                return parent.findViewHolderForAdapterPosition(nextPosition)?.itemView
            }
        }
        return null
    }

    private fun getHeaderOffset(nextHeaderView: View?, topChild: View, headerView: View): Int {
        return if (nextHeaderView != null && topChild.bottom < nextHeaderView.top - headerView.height) {
            nextHeaderView.top - headerView.height
        } else {
            0
        }
    }
}