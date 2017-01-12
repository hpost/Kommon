package cc.femto.kommon.ui.transitions

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.transition.TransitionValues
import android.transition.Visibility
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

/**
 * A transition that animates the alpha, scale X & Y of a view simultaneously.
 * See https://github.com/nickbutcher/plaid/blob/master/app/src/main/java/io/plaidapp/ui/transitions/Pop.java
 */
class Pop(context: Context, attrs: AttributeSet) : Visibility(context, attrs) {

    override fun onAppear(sceneRoot: ViewGroup, view: View, startValues: TransitionValues,
                          endValues: TransitionValues): Animator {
        view.alpha = 0f
        view.scaleX = 0f
        view.scaleY = 0f
        return ObjectAnimator.ofPropertyValuesHolder(
                view,
                PropertyValuesHolder.ofFloat(View.ALPHA, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f))
    }

    override fun onDisappear(sceneRoot: ViewGroup, view: View, startValues: TransitionValues,
                             endValues: TransitionValues): Animator {
        return ObjectAnimator.ofPropertyValuesHolder(
                view,
                PropertyValuesHolder.ofFloat(View.ALPHA, 0f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f))
    }
}