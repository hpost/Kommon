package cc.femto.kommon.mvp

interface MvpContentView : MvpView {
    fun showLoading()
    fun showContent()
    fun showEmpty()
    fun showError(msg: String)
}