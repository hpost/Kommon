package cc.femto.kommon.util

import android.support.annotation.IdRes
import android.transition.Transition
import android.transition.TransitionSet
import android.view.View
import android.view.ViewGroup
import java.util.*

/**
 * Utility methods for working with transitions
 * See https://github.com/nickbutcher/plaid/blob/master/app/src/main/java/io/plaidapp/util/TransitionUtils.java
 */
object TransitionUtils {

    fun findTransition(
            set: TransitionSet, clazz: Class<out Transition>): Transition? {
        for (i in 0..set.transitionCount - 1) {
            val transition = set.getTransitionAt(i)
            if (transition.javaClass == clazz) {
                return transition
            }
            if (transition is TransitionSet) {
                val child = findTransition(transition, clazz)
                if (child != null) return child
            }
        }
        return null
    }

    fun findTransition(
            set: TransitionSet,
            clazz: Class<out Transition>,
            @IdRes targetId: Int): Transition? {
        for (i in 0..set.transitionCount - 1) {
            val transition = set.getTransitionAt(i)
            if (transition.javaClass == clazz) {
                if (transition.targetIds.contains(targetId)) {
                    return transition
                }
            }
            if (transition is TransitionSet) {
                val child = findTransition(transition, clazz, targetId)
                if (child != null) return child
            }
        }
        return null
    }

    fun setAncestralClipping(view: View, clipChildren: Boolean): List<Boolean> {
        return setAncestralClipping(view, clipChildren, ArrayList<Boolean>())
    }

    private fun setAncestralClipping(
            view: View, clipChildren: Boolean, was: MutableList<Boolean>): List<Boolean> {
        if (view is ViewGroup) {
            was.add(view.clipChildren)
            view.clipChildren = clipChildren
        }
        val parent = view.parent
        if (parent != null && parent is ViewGroup) {
            setAncestralClipping(parent, clipChildren, was)
        }
        return was
    }

    fun restoreAncestralClipping(view: View, was: MutableList<Boolean>) {
        if (view is ViewGroup) {
            view.clipChildren = was.removeAt(0)
        }
        val parent = view.parent
        if (parent != null && parent is ViewGroup) {
            restoreAncestralClipping(parent, was)
        }
    }
}
