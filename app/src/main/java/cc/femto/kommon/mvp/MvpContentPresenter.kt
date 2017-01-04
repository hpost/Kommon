package cc.femto.kommon.mvp

abstract class MvpContentPresenter<V : MvpContentView> : MvpBasePresenter<V>() {

    /**
     * Called by the layout when the retry button was pressed
     */
    abstract fun retry()
}