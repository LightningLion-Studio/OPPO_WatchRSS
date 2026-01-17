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
    private val adapterObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            resetSwipeState()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            resetSwipeState()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            resetSwipeState()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            resetSwipeState()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            resetSwipeState()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            resetSwipeState()
        }
    }

    init {
        recyclerView.adapter?.registerAdapterDataObserver(adapterObserver)
    }

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

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 2f

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val actionWidth = resolveActionWidth(viewHolder)
        if (actionWidth > 0) {
            openSwipe(viewHolder, actionWidth)
        } else {
            resetSwipe(viewHolder)
        }
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
            if (kotlin.math.abs(clampedDx) > 0f) {
                resetPressScale(viewHolder)
            }
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
        var closed = false
        val holder = if (openPosition != RecyclerView.NO_POSITION) {
            recyclerView.findViewHolderForAdapterPosition(openPosition)
        } else {
            null
        }
        if (holder != null) {
            resetSwipe(holder)
            closed = true
        }
        activeHolder?.let { active ->
            if (active != holder) {
                resetSwipe(active)
                closed = true
            }
        }
        if (!closed) {
            closed = resetVisibleItems()
        }
        if (closed) {
            openPosition = RecyclerView.NO_POSITION
            activeHolder = null
            activeDx = 0f
        }
        return closed
    }

    private fun resetSwipeState() {
        resetVisibleItems()
        openPosition = RecyclerView.NO_POSITION
        activeHolder = null
        activeDx = 0f
    }

    private fun resetVisibleItems(): Boolean {
        var changed = false
        val childCount = recyclerView.childCount
        for (index in 0 until childCount) {
            val child = recyclerView.getChildAt(index) ?: continue
            if (resetSwipeForItemView(child)) {
                changed = true
            }
        }
        return changed
    }

    private fun resetSwipeForItemView(itemView: View): Boolean {
        val content = itemView.findViewById<View>(R.id.swipe_content) ?: return false
        var changed = false
        if (content.translationX != 0f) {
            content.translationX = 0f
            changed = true
        }
        val cover = itemView.findViewById<View>(R.id.swipe_cover)
        if (cover != null && cover.translationX != 0f) {
            cover.translationX = 0f
            changed = true
        }
        return changed
    }

    private fun settleSwipe(viewHolder: RecyclerView.ViewHolder) {
        val actionWidth = resolveActionWidth(viewHolder)
        if (actionWidth <= 0) {
            resetSwipe(viewHolder)
            return
        }
        val content = findSwipeContent(viewHolder)
        val currentDx = if (activeHolder == viewHolder && activeDx != 0f) {
            activeDx
        } else {
            content.translationX
        }
        val shouldOpen = currentDx <= -actionWidth / 2f
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

    private fun findHomeContent(viewHolder: RecyclerView.ViewHolder): View? {
        return viewHolder.itemView.findViewById(R.id.home_entry_item)
    }

    private fun findSwipeCover(viewHolder: RecyclerView.ViewHolder): View? {
        return viewHolder.itemView.findViewById(R.id.swipe_cover)
    }

    private fun translateSwipe(viewHolder: RecyclerView.ViewHolder, translationX: Float) {
        findSwipeContent(viewHolder).translationX = translationX
        findSwipeCover(viewHolder)?.translationX = translationX
    }

    private fun resetPressScale(viewHolder: RecyclerView.ViewHolder) {
        val content = findSwipeContent(viewHolder)
        resetScale(content)
        val homeContent = findHomeContent(viewHolder)
        if (homeContent != null && homeContent !== content) {
            resetScale(homeContent)
        }
        val cover = findSwipeCover(viewHolder)
        if (cover != null && cover !== content && cover !== homeContent) {
            resetScale(cover)
        }
    }

    private fun resetScale(view: View) {
        view.animate().cancel()
        view.scaleX = 1f
        view.scaleY = 1f
    }
}
