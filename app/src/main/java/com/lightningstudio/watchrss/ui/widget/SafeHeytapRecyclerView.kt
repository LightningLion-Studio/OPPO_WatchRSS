package com.lightningstudio.watchrss.ui.widget

import android.content.Context
import android.util.AttributeSet
import com.heytap.wearable.support.recycler.widget.RecyclerView

class SafeHeytapRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {
    override fun onOverScrolled(
        scrollX: Int,
        scrollY: Int,
        clampedX: Boolean,
        clampedY: Boolean
    ) {
        try {
            super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
        } catch (e: NoSuchMethodError) {
            // Avoid crash on non-OPPO environments missing platform hooks.
        }
    }
}
