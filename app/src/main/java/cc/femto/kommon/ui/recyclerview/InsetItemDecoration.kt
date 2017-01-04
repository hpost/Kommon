package cc.femto.kommon.ui.recyclerview

import android.graphics.Rect
import android.support.annotation.Dimension
import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.View

class InsetItemDecoration(@LayoutRes private val itemLayoutId: Int,
                          @Dimension private val paddingHorizontal: Int,
                          @Dimension private val paddingVertical: Int) : RecyclerView.ItemDecoration() {

    fun isDecoratedItem(child: View, parent: RecyclerView): Boolean
            = parent.layoutManager.getItemViewType(child) == itemLayoutId

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        if (!isDecoratedItem(view, parent)) {
            return
        }

        outRect.left = paddingHorizontal
        outRect.top = paddingVertical
        outRect.right = paddingHorizontal
        outRect.bottom = paddingVertical
    }
}
