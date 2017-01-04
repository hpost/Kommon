package cc.femto.kommon.util

import android.content.SharedPreferences
import com.f2prateek.rx.preferences.Preference
import com.google.gson.Gson
import java.lang.reflect.Type

/**
 * A [Preference.Adapter] implementation that serializes types as JSON strings
 */
class GsonPreferenceAdapter<T>(private val gson: Gson, private val type: Type) : Preference.Adapter<T> {

    override fun get(key: String, preferences: SharedPreferences): T
            = gson.fromJson<T>(preferences.getString(key, null), type)

    override fun set(key: String, value: T, editor: SharedPreferences.Editor) {
        val json = gson.toJson(value)
        // Add timestamp so that the value will always be unique and trigger [OnSharedPreferenceChangeListener]
        val timestampedJson = json.dropLast(1) +
                """, "__serialization_timestamp" = ${System.currentTimeMillis()} }"""
        editor.putString(key, timestampedJson)
    }
}
