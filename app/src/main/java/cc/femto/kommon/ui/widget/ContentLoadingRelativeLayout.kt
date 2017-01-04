package cc.femto.kommon.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout

/**
 * ContentLoadingRelativeLayout implements a RelativeLayout that waits a minimum time to be
 * dismissed before showing. Once visible, the view will be visible for
 * a minimum amount of time to avoid "flashes" in the UI when an event could take
 * a largely variable time to complete (from none, to a user perceivable amount)
 *
 * @see android.support.v4.widget.ContentLoadingProgressBar
 */
class ContentLoadingRelativeLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : RelativeLayout(context, attrs, 0) {

    internal var startTime: Long = -1
    internal var postedHide = false
    internal var postedShow = false
    internal var dismissed = false

    private val delayedHide = Runnable {
        postedHide = false
        startTime = -1
        visibility = View.GONE
    }

    private val delayedShow = Runnable {
        postedShow = false
        if (!dismissed) {
            startTime = System.currentTimeMillis()
            visibility = View.VISIBLE
        }
    }

    public override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        removeCallbacks()
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks()
    }

    private fun removeCallbacks() {
        removeCallbacks(delayedHide)
        removeCallbacks(delayedShow)
    }

    /**
     * Hide the view if it is visible. The view will not be
     * hidden until it has been shown for at least a minimum show time. If the
     * view was not yet visible, cancels showing the view.
     */
    fun hide() {
        dismissed = true
        removeCallbacks(delayedShow)
        val diff = System.currentTimeMillis() - startTime
        if (diff >= MIN_SHOW_TIME || startTime == -1L) {
            // The progress spinner has been shown long enough
            // OR was not shown yet. If it wasn't shown yet,
            // it will just never be shown.
            visibility = View.GONE
        } else {
            // The progress spinner is shown, but not long enough,
            // so put a delayed message in to hide it when its been
            // shown long enough.
            if (!postedHide) {
                postDelayed(delayedHide, MIN_SHOW_TIME - diff)
                postedHide = true
            }
        }
    }

    /**
     * Show the view after waiting for a minimum delay.
     * If during that time, hide() is called, the view is never made visible.
     */
    fun show() {
        // Reset the start time.
        startTime = -1
        dismissed = false
        removeCallbacks(delayedHide)
        if (!postedShow) {
            postDelayed(delayedShow, MIN_DELAY.toLong())
            postedShow = true
        }
    }

    companion object {
        private val MIN_SHOW_TIME = 500 // ms
        private val MIN_DELAY = 500 // ms
    }
}

