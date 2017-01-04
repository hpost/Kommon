package cc.femto.kommon.ui.recyclerview

import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View

open class MarginDecoration : RecyclerView.ItemDecoration {

    private var margin = -1

    private var marginLeft: Int = 0

    private var marginTop: Int = 0

    private var marginRight: Int = 0

    private var marginBottom: Int = 0

    constructor(margin: Int) {
        this.margin = margin
    }

    constructor(marginLeft: Int, marginTop: Int, marginRight: Int, marginBottom: Int) {
        this.marginLeft = marginLeft
        this.marginTop = marginTop
        this.marginRight = marginRight
        this.marginBottom = marginBottom
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        if (margin != -1) {
            outRect.set(margin, margin, margin, margin)
        } else {
            outRect.set(marginLeft, marginTop, marginRight, marginBottom)
        }
    }
}
