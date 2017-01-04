package cc.femto.kommon.ui.recyclerview

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import rx.Observable
import rx.subjects.PublishSubject
import java.util.concurrent.TimeUnit

/**
 * Inspired by https://gist.github.com/sebnapi/fde648c17616d9d3bcde
 *
 * If you extend this Adapter you are able to add a Header, a Footer or both by a similar ViewHolder pattern as in
 * RecyclerView.
 * Don't override (Be careful while overriding) - onCreateViewHolder - onBindViewHolder -
 * getItemCount - getItemViewType
 * You need to override the abstract methods introduced by this class. This class is
 * not using generics as RecyclerView.Adapter. Make yourself sure to cast right.
 */
abstract class AdvancedRecyclerViewAdapter(val paginationListener: OnPaginationListener? = null)
: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        val PAGINATION_OFFSET = 10
        val TYPE_HEADER = Integer.MIN_VALUE
        val TYPE_FOOTER = Integer.MIN_VALUE + 1
        val TYPE_ADAPTEE_OFFSET = 2
    }

    interface OnPaginationListener {
        fun onPagination()
    }

    private var headerViewHolder: RecyclerView.ViewHolder? = null
    private var footerViewHolder: RecyclerView.ViewHolder? = null
    private var paginationSubject: PublishSubject<Void>? = null

    /**
     * Returns an Observable that emits items once the pagination threshold was reached. It is throttled to avoid
     * excessive pagination.
     */
    val paginationObservable: Observable<Void>
        get() {
            if (paginationSubject == null) {
                paginationSubject = PublishSubject.create<Void>()
            }
            return paginationSubject!!.asObservable().throttleFirst(500, TimeUnit.MILLISECONDS)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_HEADER && headerViewHolder != null) {
            return headerViewHolder!!
        } else if (viewType == TYPE_FOOTER && footerViewHolder != null) {
            return footerViewHolder!!
        }
        return onCreateBasicItemViewHolder(parent, viewType - TYPE_ADAPTEE_OFFSET)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        if (paginationListener != null
                && getBasicItemCount() != 0 //don't callback when list is empty
                && position >= itemCount - 1 - PAGINATION_OFFSET) {
            paginationListener.onPagination()
        }

        if (paginationSubject != null && paginationSubject!!.hasObservers()
                && getBasicItemCount() != 0 //don't callback when list is empty
                && position >= itemCount - 1 - PAGINATION_OFFSET) {
            paginationSubject!!.onNext(null)
        }

        if (position == 0 && holder!!.itemViewType == TYPE_HEADER) {
            //no need to bind, header is not being recycled
            return
        }

        val last = itemCount - 1
        if (position == last && holder!!.itemViewType == TYPE_FOOTER) {
            //no need to bind, footer is not being recycled
            return
        }

        if (holder != null) {
            val headerOffset = if (headerViewHolder != null) 1 else 0
            onBindBasicItemView(holder, position - headerOffset)
        }
    }

    fun offsetPosition(position: Int): Int = if (headerViewHolder != null) position - 1 else position

    override fun getItemCount(): Int {
        var itemCount = getBasicItemCount()
        if (headerViewHolder != null) {
            itemCount += 1
        }
        if (footerViewHolder != null) {
            itemCount += 1
        }
        return itemCount
    }

    val isEmpty: Boolean
        get() = getBasicItemCount() == 0

    override fun getItemViewType(position: Int): Int {
        var position = position
        if (position == 0 && headerViewHolder != null) {
            return TYPE_HEADER
        }
        val last = itemCount - 1
        if (position == last && footerViewHolder != null) {
            return TYPE_FOOTER
        }
        //if header exists, readjust position to pass back the true BasicItemViewType position
        if (headerViewHolder != null) {
            position--
        }

        if (getBasicItemViewType(position) >= Integer.MAX_VALUE - TYPE_ADAPTEE_OFFSET) {
            throw IllegalStateException(
                    "AdvancedRecyclerViewAdapter offsets your BasicItemType by $TYPE_ADAPTEE_OFFSET.")
        }
        return getBasicItemViewType(position) + TYPE_ADAPTEE_OFFSET
    }

    fun setHeaderView(view: View?) {
        if (view == null) {
            headerViewHolder = null
            return
        }
        //create concrete instance of abstract ViewHolder class
        val viewHolder = object : RecyclerView.ViewHolder(view) {

        }
        headerViewHolder = viewHolder
    }

    fun setFooterView(view: View?) {
        if (view == null) {
            footerViewHolder = null
            return
        }
        //create concrete instance of abstract ViewHolder class
        val viewHolder = object : RecyclerView.ViewHolder(view) {

        }
        footerViewHolder = viewHolder
    }

    protected abstract fun onCreateBasicItemViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder

    protected abstract fun onBindBasicItemView(holder: RecyclerView.ViewHolder, position: Int)

    abstract fun getBasicItemCount(): Int

    /**
     * Override when multiple row types are supported.
     * Make sure you don't use [Integer.MIN_VALUE, Integer.MIN_VALUE + 1]
     * or Integer.MAX_VALUE as BasicItemViewType
     */
    protected open fun getBasicItemViewType(position: Int): Int {
        return 0
    }
}