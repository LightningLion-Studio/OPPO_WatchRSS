package com.lightningstudio.watchrss.ui.util

import android.graphics.Canvas
import android.view.View
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.heytap.wearable.support.recycler.widget.helper.ItemTouchHelper
import com.lightningstudio.watchrss.R

class SwipeRevealCallback(
    private val recyclerView: RecyclerView,
    private val canSwipe: (RecyclerView.ViewHolder) -> Boolean
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
    private var openPosition = RecyclerView.NO_POSITION
    private var activeHolder: RecyclerView.ViewHolder? = null
    private var activeDx: Float = 0f

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun getSwipeDirs(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return if (canSwipe(viewHolder)) ItemTouchHelper.LEFT else 0
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 1f

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        resetSwipe(viewHolder)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        val actionWidth = resolveActionWidth(viewHolder)
        if (actionWidth <= 0) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        if (viewHolder.adapterPosition != openPosition) {
            closeOpenItem()
        }

        val clampedDx = dX.coerceIn(-actionWidth.toFloat(), 0f)
        if (isCurrentlyActive) {
            activeHolder = viewHolder
            activeDx = clampedDx
            translateSwipe(viewHolder, clampedDx)
        } else if (activeHolder == viewHolder) {
            translateSwipe(viewHolder, activeDx)
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        settleSwipe(viewHolder)
        if (activeHolder == viewHolder) {
            activeHolder = null
        }
    }

    fun closeOpenItem(): Boolean {
        if (openPosition == RecyclerView.NO_POSITION) return false
        val holder = recyclerView.findViewHolderForAdapterPosition(openPosition)
        if (holder == null) {
            openPosition = RecyclerView.NO_POSITION
            return false
        }
        resetSwipe(holder)
        return true
    }

    private fun settleSwipe(viewHolder: RecyclerView.ViewHolder) {
        val actionWidth = resolveActionWidth(viewHolder)
        if (actionWidth <= 0) {
            resetSwipe(viewHolder)
            return
        }
        val content = findSwipeContent(viewHolder)
        val shouldOpen = content.translationX <= -actionWidth / 2f
        if (shouldOpen) {
            openSwipe(viewHolder, actionWidth)
        } else {
            resetSwipe(viewHolder)
        }
    }

    private fun openSwipe(viewHolder: RecyclerView.ViewHolder, actionWidth: Int) {
        translateSwipe(viewHolder, -actionWidth.toFloat())
        openPosition = viewHolder.adapterPosition
        activeHolder = viewHolder
        activeDx = -actionWidth.toFloat()
    }

    private fun resetSwipe(viewHolder: RecyclerView.ViewHolder) {
        translateSwipe(viewHolder, 0f)
        if (openPosition == viewHolder.adapterPosition) {
            openPosition = RecyclerView.NO_POSITION
        }
        if (activeHolder == viewHolder) {
            activeHolder = null
            activeDx = 0f
        }
    }

    private fun resolveActionWidth(viewHolder: RecyclerView.ViewHolder): Int {
        val actions = viewHolder.itemView.findViewById<View>(R.id.swipe_actions) ?: return 0
        return if (actions.width > 0) actions.width else actions.measuredWidth
    }

    private fun findSwipeContent(viewHolder: RecyclerView.ViewHolder): View {
        return viewHolder.itemView.findViewById(R.id.swipe_content) ?: viewHolder.itemView
    }

    private fun findSwipeCover(viewHolder: RecyclerView.ViewHolder): View? {
        return viewHolder.itemView.findViewById(R.id.swipe_cover)
    }

    private fun translateSwipe(viewHolder: RecyclerView.ViewHolder, translationX: Float) {
        findSwipeContent(viewHolder).translationX = translationX
        findSwipeCover(viewHolder)?.translationX = translationX
    }
}
