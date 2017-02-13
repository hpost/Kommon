package cc.femto.kommon.ui.activity

import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.view.ViewGroup
import rx.Observable
import rx.lang.kotlin.PublishSubject
import rx.subjects.PublishSubject

abstract class ViewContainerActivity : BaseActivity() {

    private val activityResultSubjects: MutableMap<Int, PublishSubject<Instrumentation.ActivityResult>> = mutableMapOf()

    protected var contentView: ViewGroup? = null
        private set

    /**
     * Called in ViewContainerActivity#onCreate.
     * @return The resource ID of the view that should be embedded in this Activity
     */
    abstract val viewId: Int

    fun startActivityForResultObservable(intent: Intent, requestCode: Int, options: Bundle? = null): Observable<Instrumentation.ActivityResult> {
        val subject = PublishSubject<Instrumentation.ActivityResult>()
        activityResultSubjects.put(requestCode, subject)
        super.startActivityForResult(intent, requestCode, options)
        return subject.asObservable()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contentView = layoutInflater.inflate(viewId, null, false) as ViewGroup
        setContentView(contentView)

        if (contentView is ActivityLifecycleListener) {
            (contentView as ActivityLifecycleListener).onActivityCreate(savedInstanceState)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        if (contentView is ActivityLifecycleListener) {
            (contentView as ActivityLifecycleListener).onActivityRestoreInstanceState(savedInstanceState)
        }
    }

    override fun onPause() {
        super.onPause()
        if (contentView is ActivityLifecycleListener) {
            (contentView as ActivityLifecycleListener).onActivityPause()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (contentView is ActivityLifecycleListener) {
            (contentView as ActivityLifecycleListener).onActivityLowMemory()
        }
    }

    override fun onResume() {
        super.onResume()
        if (contentView is ActivityLifecycleListener) {
            (contentView as ActivityLifecycleListener).onActivityResume()
        }
    }

    override fun onDestroy() {
        activityResultSubjects.clear()
        super.onDestroy()
        if (contentView is ActivityLifecycleListener) {
            (contentView as ActivityLifecycleListener).onActivityDestroy()
        }
    }

    override fun onStart() {
        super.onStart()
        if (contentView is ActivityLifecycleListener) {
            (contentView as ActivityLifecycleListener).onActivityStart()
        }
    }

    override fun onStop() {
        super.onStop()
        if (contentView is ActivityLifecycleListener) {
            (contentView as ActivityLifecycleListener).onActivityStop()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (contentView is ActivityLifecycleListener) {
            (contentView as ActivityLifecycleListener).onActivitySaveInstanceState(outState)
        }
    }

    override fun onNewIntent(intent: Intent) {
        if (contentView is OnNewIntentListener) {
            (contentView as OnNewIntentListener).onNewIntent(intent)
        }
        super.onNewIntent(intent)
    }

    override fun onBackPressed() {
        var consumed = false
        if (contentView is OnBackPressedListener) {
            consumed = (contentView as OnBackPressedListener).onBackPressed()
        }
        if (!consumed) {
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        activityResultSubjects[requestCode]?.onNext(Instrumentation.ActivityResult(resultCode, data))
        if (contentView is OnActivityResultListener) {
            (contentView as OnActivityResultListener).onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (contentView is ActivityCompat.OnRequestPermissionsResultCallback) {
            (contentView as ActivityCompat.OnRequestPermissionsResultCallback).onRequestPermissionsResult(requestCode,
                    permissions, grantResults)
        }
    }

    interface OnBackPressedListener {
        fun onBackPressed(): Boolean
    }

    interface OnNewIntentListener {
        fun onNewIntent(intent: Intent)
    }

    interface OnActivityResultListener {
        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    }

    interface ActivityLifecycleListener {
        fun onActivityCreate(savedInstanceState: Bundle?) {
        }

        fun onActivityStart() {
        }

        fun onActivityRestoreInstanceState(savedInstanceState: Bundle?) {
        }

        fun onActivityResume() {
        }

        fun onActivitySaveInstanceState(outBundle: Bundle?) {
        }

        fun onActivityPause() {
        }

        fun onActivityStop() {
        }

        fun onActivityDestroy() {
        }

        fun onActivityLowMemory() {
        }
    }
}
