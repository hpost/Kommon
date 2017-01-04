package cc.femto.kommon.ui.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.util.AttributeSet
import android.util.Property
import android.util.TypedValue
import android.view.*
import android.widget.FrameLayout
import cc.femto.kommon.R

/**
 * Forked from {@see https://github.com/Flipboard/bottomsheet}
 */
class BottomSheetLayout : FrameLayout {

    companion object {
        private val SHEET_TRANSLATION =
                object : Property<BottomSheetLayout, Float>(Float::class.java, "sheetTranslation") {
                    override fun get(obj: BottomSheetLayout): Float {
                        return obj.sheetTranslation
                    }

                    override fun set(obj: BottomSheetLayout, value: Float?) {
                        obj.sheetTranslation = value ?: 0f
                    }
                }
        val MAX_DIM_ALPHA = 0.7f
        private val ANIMATION_DURATION: Long = 300
        private val DISMISS_THRESHOLD_DIP = 56
    }

    interface ActionListener {
        fun showAsBottomSheet(view: View)
        fun dismissBottomSheet()
    }

    /**
     * Utility class which registers if the animation has been canceled so that subclasses may respond differently in
     * onAnimationEnd
     */
    private open inner class CancelDetectionAnimationListener : AnimatorListenerAdapter() {

        protected var canceled: Boolean = false

        override fun onAnimationCancel(animation: Animator) {
            canceled = true
        }

    }

    enum class State {
        HIDDEN,
        PEEKED,
        EXPANDED
    }

    interface OnSheetStateChangeListener {
        fun onSheetStateChanged(state: State)
    }

    interface OnSheetDismissedListener {
        /**
         * Called when the presented sheet has been dismissed.
         * @param bottomSheetLayout The bottom sheet which contained the presented sheet.
         */
        fun onDismissed(bottomSheetLayout: BottomSheetLayout)

    }

    private var dismissThreshold: Int = 0

    private val contentClipRect = Rect()

    /**
     * @return The current state of the sheet.
     */
    var state = State.HIDDEN
        private set(state) {
            field = state
            if (onSheetStateChangeListener != null) {
                onSheetStateChangeListener!!.onSheetStateChanged(state)
            }
        }

    private val animationInterpolator = FastOutSlowInInterpolator()

    var bottomSheetOwnsTouch: Boolean = false

    private var sheetViewOwnsTouch: Boolean = false

    private var sheetTranslation: Float = 0.toFloat()
        set(sheetTranslation) {
            field = sheetTranslation
            val bottomClip = (height - Math.ceil(sheetTranslation.toDouble())).toInt()
            this.contentClipRect.set(0, 0, width, bottomClip)
            sheetView!!.translationY = height - sheetTranslation
            dimView!!.alpha = if (shouldDimContentView) getDimAlpha(sheetTranslation) else 0f
        }

    private var velocityTracker: VelocityTracker? = null

    private var minFlingVelocity: Float = 0.toFloat()

    private var touchSlop: Float = 0.toFloat()

    private var onSheetDismissedListener: OnSheetDismissedListener? = null

    private var shouldDimContentView = true

    private var useHardwareLayerWhileAnimating = true

    private var currentAnimator: Animator? = null

    private var onSheetStateChangeListener: OnSheetStateChangeListener? = null

    private var sheetViewOnLayoutChangeListener: OnLayoutChangeListener? = null

    private var dimView: View? = null

    /**
     * @return true if we are intercepting content view touches or false to allow interaction with Bottom Sheet's
     * * content view. Default value is true.
     */

    /**
     * Controls whether or not child view interaction is possible when the bottom sheet is open.
     * true to intercept content view touches or false to allow interaction with Bottom Sheet's content view
     */
    var interceptContentTouch = true

    private var currentSheetViewHeight: Int = 0

    private var hasIntercepted: Boolean = false

    /**
     * Some values we need to manage width on tablets
     */
    private var screenWidth = 0

    private val isTablet = resources.getBoolean(R.bool.is_large_screen)

    private val defaultSheetWidth = resources.getDimensionPixelSize(R.dimen.bottom_sheet_width)

    private var sheetStartX = 0

    private var sheetEndX = 0


    /**
     * Snapshot of the touch's y position on a down event
     */
    private var downY: Float = 0.toFloat()

    /**
     * Snapshot of the touch's x position on a down event
     */
    private var downX: Float = 0.toFloat()

    /**
     * Snapshot of the sheet's translation at the time of the last down event
     */
    private var downSheetTranslation: Float = 0.toFloat()

    /**
     * Snapshot of the sheet's state at the time of the last down event
     */
    private var downState: State? = null

    constructor(context: Context) : super(context) {
        init()
    }

    @JvmOverloads constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {
        init()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    private fun init() {
        val viewConfiguration = ViewConfiguration.get(context)
        minFlingVelocity = viewConfiguration.scaledMinimumFlingVelocity.toFloat()
        touchSlop = viewConfiguration.scaledTouchSlop.toFloat()

        dismissThreshold = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DISMISS_THRESHOLD_DIP.toFloat(),
                context.resources.displayMetrics).toInt()

        dimView = View(context)
        dimView!!.setBackgroundColor(Color.BLACK)
        dimView!!.alpha = 0f
        super.addView(dimView, -1, generateDefaultLayoutParams())

        isFocusableInTouchMode = true

        val point = Point()
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getSize(point)
        screenWidth = point.x
    }

    /**
     * Don't call addView directly, use setContentView() and showWithSheetView()
     */
    override fun addView(child: View) {
        if (childCount > 0) {
            throw IllegalArgumentException(
                    "You may not declare more than one child of bottom sheet. The sheet view must be added dynamically with showWithSheetView()")
        }
        setContentView(child)
    }

    override fun addView(child: View, index: Int) {
        addView(child)
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        addView(child)
    }

    override fun addView(child: View, params: ViewGroup.LayoutParams) {
        addView(child)
    }

    override fun addView(child: View, width: Int, height: Int) {
        addView(child)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        velocityTracker = VelocityTracker.obtain()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        velocityTracker!!.clear()
        cancelCurrentAnimation()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val bottomClip = (height - Math.ceil(sheetTranslation.toDouble())).toInt()
        this.contentClipRect.set(0, 0, width, bottomClip)
    }

    private fun getDimAlpha(translation: Float): Float {
        val progress = translation / maxSheetTranslation
        return progress * MAX_DIM_ALPHA
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val downAction = ev.actionMasked == MotionEvent.ACTION_DOWN
        if (downAction) {
            hasIntercepted = false
        }
        if (interceptContentTouch || ev.y > height - sheetTranslation && isXInSheet(ev.x)) {
            hasIntercepted = downAction && isSheetShowing
        }
        return hasIntercepted
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isSheetShowing) {
            return false
        }
        if (isAnimating) {
            return false
        }
        if (!hasIntercepted) {
            return onInterceptTouchEvent(event)
        }
        if (event.action == MotionEvent.ACTION_DOWN) {
            // Snapshot the state of things when finger touches the screen.
            // This allows us to calculate deltas without losing precision which we would have if we calculated deltas based on the previous touch.
            bottomSheetOwnsTouch = false
            sheetViewOwnsTouch = false
            downY = event.y
            downX = event.x
            downSheetTranslation = sheetTranslation
            downState = state
            velocityTracker!!.clear()
        }
        velocityTracker!!.addMovement(event)

        // The max translation is a hard limit while the min translation is where we start dragging more slowly and allow the sheet to be dismissed.
        val maxSheetTranslation = maxSheetTranslation
        val peekSheetTranslation = maxSheetTranslation - dismissThreshold

        var deltaY = downY - event.y
        var deltaX = downX - event.x

        if (!bottomSheetOwnsTouch && !sheetViewOwnsTouch) {
            bottomSheetOwnsTouch = Math.abs(deltaY) > touchSlop
            sheetViewOwnsTouch = Math.abs(deltaX) > touchSlop

            if (bottomSheetOwnsTouch) {
                if (state == State.PEEKED) {
                    val cancelEvent = MotionEvent.obtain(event)
                    cancelEvent.offsetLocation(0f, sheetTranslation - height)
                    cancelEvent.action = MotionEvent.ACTION_CANCEL
                    sheetView!!.dispatchTouchEvent(cancelEvent)
                    cancelEvent.recycle()
                }

                sheetViewOwnsTouch = false
                downY = event.y
                downX = event.x
                deltaY = 0f
                deltaX = 0f
            }
        }

        // This is not the actual new sheet translation but a first approximation it will be adjusted to account for max and min translations etc.
        var newSheetTranslation = downSheetTranslation + deltaY

        if (bottomSheetOwnsTouch) {
            // If we are scrolling down and the sheet cannot scroll further, go out of expanded mode.
            val scrollingDown = deltaY < 0
            val canScrollUp = canScrollUp(sheetView!!, event.x, event.y + (sheetTranslation - height))
            if (state == State.EXPANDED && scrollingDown && !canScrollUp) {
                // Reset variables so deltas are correctly calculated from the point at which the sheet was 'detached' from the top.
                downY = event.y
                downSheetTranslation = sheetTranslation
                velocityTracker!!.clear()
                state = State.PEEKED
                setSheetLayerTypeIfEnabled(View.LAYER_TYPE_HARDWARE)
                newSheetTranslation = sheetTranslation

                // Dispatch a cancel event to the sheet to make sure its touch handling is cleaned up nicely.
                val cancelEvent = MotionEvent.obtain(event)
                cancelEvent.action = MotionEvent.ACTION_CANCEL
                sheetView!!.dispatchTouchEvent(cancelEvent)
                cancelEvent.recycle()
            }

            // If we are at the top of the view we should go into expanded mode.
            if (state == State.PEEKED && newSheetTranslation > maxSheetTranslation) {
                sheetTranslation = maxSheetTranslation

                // Dispatch a down event to the sheet to make sure its touch handling is initiated correctly.
                newSheetTranslation = Math.min(maxSheetTranslation, newSheetTranslation)
                val downEvent = MotionEvent.obtain(event)
                downEvent.action = MotionEvent.ACTION_DOWN
                sheetView!!.dispatchTouchEvent(downEvent)
                downEvent.recycle()
                state = State.EXPANDED
                setSheetLayerTypeIfEnabled(View.LAYER_TYPE_NONE)
            }

            if (state == State.EXPANDED) {
                // Dispatch the touch to the sheet if we are expanded so it can handle its own internal scrolling.
                event.offsetLocation(0f, sheetTranslation - height)
                sheetView!!.dispatchTouchEvent(event)
            } else {
                // Make delta less effective when sheet is below the minimum translation.
                // This makes it feel like scrolling in jello which gives the user an indication that the sheet will be dismissed if they let go.
                if (newSheetTranslation < maxSheetTranslation) {
                    newSheetTranslation = maxSheetTranslation - (maxSheetTranslation - newSheetTranslation) / 4f
                }

                sheetTranslation = newSheetTranslation

                if (event.action == MotionEvent.ACTION_CANCEL) {
                    // If touch is canceled, go back to previous state, a canceled touch should never commit an action.
                    if (downState == State.EXPANDED) {
                        expandSheet()
                    } else {
                        dismissSheet()
                    }
                }

                if (event.action == MotionEvent.ACTION_UP) {
                    if (newSheetTranslation < peekSheetTranslation) {
                        dismissSheet()
                    } else {
                        // If touch is released, go to a new state depending on velocity.
                        // If the velocity is not high enough we use the position of the sheet to determine the new state.
                        velocityTracker!!.computeCurrentVelocity(1000)
                        val velocityY = velocityTracker!!.yVelocity
                        if (Math.abs(velocityY) < minFlingVelocity) {
                            if (sheetTranslation > height / 2) {
                                expandSheet()
                            } else {
                                dismissSheet()
                            }
                        } else {
                            if (velocityY < 0) {
                                expandSheet()
                            } else {
                                dismissSheet()
                            }
                        }
                    }
                }
            }
        } else {
            // If the user clicks outside of the bottom sheet area we should dismiss the bottom sheet.
            val touchOutsideBottomSheet = event.y < height - sheetTranslation || !isXInSheet(
                    event.x)
            if (event.action == MotionEvent.ACTION_UP && touchOutsideBottomSheet && interceptContentTouch) {
                dismissSheet()
                return true
            }

            event.offsetLocation(if (isTablet) x - sheetStartX else 0f, sheetTranslation - height)
            sheetView!!.dispatchTouchEvent(event)
        }
        return true
    }

    private fun isXInSheet(x: Float): Boolean {
        return !isTablet || x > sheetStartX && x < sheetEndX
    }

    private val isAnimating: Boolean
        get() = currentAnimator != null

    private fun cancelCurrentAnimation() {
        if (currentAnimator != null) {
            currentAnimator!!.cancel()
        }
    }

    private fun canScrollUp(view: View, x: Float, y: Float): Boolean {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val childLeft = child.left
                val childTop = child.top
                val childRight = child.right
                val childBottom = child.bottom
                val intersects = x > childLeft && x < childRight && y > childTop && y < childBottom
                if (intersects && canScrollUp(child, x - childLeft, y - childTop)) {
                    return true
                }
            }
        }
        return view.canScrollVertically(-1)
    }

    private fun setSheetLayerTypeIfEnabled(layerType: Int) {
        if (useHardwareLayerWhileAnimating) {
            sheetView!!.setLayerType(layerType, null)
        }
    }

    private fun hasFullHeightSheet(): Boolean {
        return sheetView == null || sheetView!!.height == height
    }

    /**
     * Set dim and translation to the initial state
     */
    private fun initializeSheetValues() {
        this.sheetTranslation = 0f
        this.contentClipRect.set(0, 0, width, height)
        sheetView!!.translationY = height.toFloat()
        dimView!!.alpha = 0f
    }

    /**
     * Set the presented sheet to be in an expanded state.
     */
    fun expandSheet() {
        cancelCurrentAnimation()
        setSheetLayerTypeIfEnabled(View.LAYER_TYPE_NONE)
        val anim = ObjectAnimator.ofFloat<BottomSheetLayout>(this, SHEET_TRANSLATION, sheetView!!.height.toFloat())
        anim.duration = ANIMATION_DURATION
        anim.interpolator = animationInterpolator
        anim.addListener(object : CancelDetectionAnimationListener() {
            override fun onAnimationEnd(animation: Animator) {
                if (!canceled) {
                    currentAnimator = null
                }
            }
        })
        anim.start()
        currentAnimator = anim
        state = State.EXPANDED
    }

    /**
     * @return The maximum translation for the presented sheet view. Translation is counted from the bottom of the view.
     */
    val maxSheetTranslation: Float
        get() = (if (hasFullHeightSheet()) height - paddingTop else sheetView!!.height).toFloat()

    /**
     * @return The currently presented sheet view. If no sheet is currently presented null will returned.
     */
    val sheetView: View?
        get() = if (childCount > 1) getChildAt(1) else null

    /**
     * Set the content view of the bottom sheet. This is the view which is shown under the sheet being presented. This
     * is usually the root view of your application.

     * @param contentView The content view of your application.
     */
    fun setContentView(contentView: View) {
        super.addView(contentView, -1, generateDefaultLayoutParams())
    }

    /**
     * Present a sheet view to the user.

     * @param sheetView                The sheet to be presented.
     * *
     * @param onSheetDismissedListener The listener to notify when the sheet is dismissed.
     */
    @JvmOverloads fun showWithSheetView(sheetView: View?, onSheetDismissedListener: OnSheetDismissedListener? = null) {
        if (state != State.HIDDEN) {
            return
        }
        var params: LayoutParams? = sheetView?.layoutParams as LayoutParams
        if (params == null) {
            params = LayoutParams(if (isTablet) LayoutParams.WRAP_CONTENT else LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL)
        }

        if (isTablet && params.width == LayoutParams.WRAP_CONTENT) {

            // Center by default if they didn't specify anything
            if (params.gravity == -1) {
                params.gravity = Gravity.CENTER_HORIZONTAL
            }

            params.width = defaultSheetWidth

            // Update start and end coordinates for touch reference
            val horizontalSpacing = screenWidth - defaultSheetWidth
            sheetStartX = horizontalSpacing / 2
            sheetEndX = screenWidth - sheetStartX
        }

        super.addView(sheetView, -1, params)
        initializeSheetValues()
        this.onSheetDismissedListener = onSheetDismissedListener

        // Don't start animating until the sheet has been drawn once. This ensures that we don't do layout while animating and that
        // the drawing cache for the view has been warmed up. tl;dr it reduces lag.
        viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                viewTreeObserver.removeOnPreDrawListener(this)
                post {
                    // Make sure sheet view is still here when first draw happens.
                    // In the case of a large lag it could be that the view is dismissed before it is drawn resulting in sheet view being null here.
                    if (sheetView != null) {
                        expandSheet()
                    }
                }
                return true
            }
        })

        // sheetView should always be anchored to the bottom of the screen
        currentSheetViewHeight = sheetView!!.measuredHeight
        sheetViewOnLayoutChangeListener = View.OnLayoutChangeListener { sheetView, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val newSheetViewHeight = sheetView.measuredHeight
            if (state != State.HIDDEN && newSheetViewHeight < currentSheetViewHeight) {
                // The sheet can no longer be in the expanded state if it has shrunk
                if (state == State.EXPANDED) {
                    state = State.PEEKED
                }
                sheetTranslation = newSheetViewHeight.toFloat()
            }
            currentSheetViewHeight = newSheetViewHeight
        }
        sheetView.addOnLayoutChangeListener(sheetViewOnLayoutChangeListener)
    }

    /**
     * Dismiss the sheet currently being presented.
     */
    fun dismissSheet() {
        if (state == State.HIDDEN) {
            // no-op
            return
        }
        cancelCurrentAnimation()
        val anim = ObjectAnimator.ofFloat<BottomSheetLayout>(this, SHEET_TRANSLATION, 0f)
        anim.duration = ANIMATION_DURATION
        anim.interpolator = animationInterpolator
        anim.addListener(object : CancelDetectionAnimationListener() {
            override fun onAnimationEnd(animation: Animator) {
                if (!canceled) {
                    currentAnimator = null
                    state = State.HIDDEN
                    setSheetLayerTypeIfEnabled(View.LAYER_TYPE_NONE)
                    removeView(sheetView)

                    if (onSheetDismissedListener != null) {
                        onSheetDismissedListener!!.onDismissed(this@BottomSheetLayout)
                    }

                    // Remove sheet specific properties
                    onSheetDismissedListener = null
                }
            }
        })
        anim.start()
        currentAnimator = anim
        //        sheetStartX = 0;
        //        sheetEndX = 0;
    }

    /**
     * @return Whether or not a sheet is currently presented.
     */
    val isSheetShowing: Boolean
        get() = state != State.HIDDEN

    /**
     * Enable or disable dimming of the content view while a sheet is presented. If enabled a transparent black dim is
     * overlaid on top of the content view indicating that the sheet is the foreground view. This dim is animated into
     * place is coordination with the sheet view. Defaults to true.

     * @param shouldDimContentView whether or not to dim the content view.
     */
    fun setShouldDimContentView(shouldDimContentView: Boolean) {
        this.shouldDimContentView = shouldDimContentView
    }

    /**
     * @return whether the content view is being dimmed while presenting a sheet or not.
     */
    fun shouldDimContentView(): Boolean {
        return shouldDimContentView
    }

    /**
     * Enable or disable the use of a hardware layer for the presented sheet while animating. This settings defaults to
     * true and should only be changed if you know that putting the sheet in a layer will negatively effect performance.
     * One such example is if the sheet contains a view which needs to frequently be re-drawn.

     * @param useHardwareLayerWhileAnimating whether or not to use a hardware layer.
     */
    fun setUseHardwareLayerWhileAnimating(useHardwareLayerWhileAnimating: Boolean) {
        this.useHardwareLayerWhileAnimating = useHardwareLayerWhileAnimating
    }

    /**
     * Set a OnSheetStateChangeListener which will be notified when the state of the presented sheet changes.

     * @param onSheetStateChangeListener the listener to be notified.
     */
    fun setOnSheetStateChangeListener(onSheetStateChangeListener: OnSheetStateChangeListener) {
        this.onSheetStateChangeListener = onSheetStateChangeListener
    }
}
