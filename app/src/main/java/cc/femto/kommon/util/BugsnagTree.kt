package cc.femto.kommon.util

import android.util.Log
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Error
import com.bugsnag.android.MetaData
import com.bugsnag.android.Severity
import timber.log.Timber
import java.util.*

/**
 * A logging implementation which buffers the last 200 messages
 * and notifies on info, warning and error logs.
 *
 * Based on https://github.com/JakeWharton/Telecine
 */
class BugsnagTree : Timber.Tree() {

    companion object {
        val BUFFER_SIZE = 200

        // Adding one to the initial size accounts for the add before remove.
        private val buffer: Deque<String> = ArrayDeque<String>(BUFFER_SIZE + 1)
    }

    override fun log(priority: Int, tag: String?, message: String?, t: Throwable?) {
        val logMessage = "${System.currentTimeMillis()} ${priorityToString(priority)} $message"
        synchronized(buffer) {
            buffer.addLast(logMessage)
            if (buffer.size > BUFFER_SIZE) {
                buffer.removeFirst()
            }
        }

        val throwable = t ?: Exception(message)
        when (priority) {
            Log.ERROR -> Bugsnag.notify(throwable, Severity.WARNING)
            Log.WARN -> Bugsnag.notify(throwable, Severity.INFO)
        }
    }

    fun update(error: Error) {
        if (error.metaData == null) {
            error.metaData = MetaData()
        }
        synchronized(buffer) {
            var i = 1
            for (message in buffer) {
                error.addToTab("Log", String.format(Locale.US, "%03d", i++), message)
            }
        }
    }

    private fun priorityToString(priority: Int) = when (priority) {
        Log.ERROR -> "E"
        Log.WARN -> "W"
        Log.INFO -> "I"
        Log.DEBUG -> "D"
        else -> priority.toString()
    }
}
