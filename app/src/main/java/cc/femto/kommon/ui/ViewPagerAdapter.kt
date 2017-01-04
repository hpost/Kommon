package cc.femto.kommon.ui

import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup

/**
 * A [PagerAdapter] that returns a view corresponding to one of the sections/tabs/pages.
 * This provides the data for the [ViewPager].
 */
abstract class ViewPagerAdapter() : PagerAdapter() {

    /**
     * Get view corresponding to a specific position.
     * @param position Position to fetch view for.
     * @return View for specified position.
     */
    abstract fun getItem(position: Int): View?

    /**
     * Get number of pages the [ViewPager] should render.
     * @return Number of views to be rendered as pages.
     */
    abstract override fun getCount(): Int

    override fun instantiateItem(container: ViewGroup, position: Int): Any? {
        val view = getItem(position)
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        container.removeView(obj as View)
    }

    override fun isViewFromObject(view: View, obj: Any): Boolean {
        return view === obj
    }
}
