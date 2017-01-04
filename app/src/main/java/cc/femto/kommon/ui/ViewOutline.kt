package cc.femto.kommon.ui

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider

/** Used for views shown in BottomSheetLayout */
class ShadowOutline(val width: Int, val height: Int) : ViewOutlineProvider() {
    override fun getOutline(view: View?, outline: Outline?) {
        outline?.setRect(0, 0, width, height)
    }
}
