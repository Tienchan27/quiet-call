package vn.quietcall.data

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArraySet

/** Activity and Telecom service run in the same app process. No exported broadcast is needed. */
object HistoryUpdates {
    private val observers = CopyOnWriteArraySet<() -> Unit>()
    private val main = Handler(Looper.getMainLooper())
    fun addObserver(observer: () -> Unit) { observers.add(observer) }
    fun removeObserver(observer: () -> Unit) { observers.remove(observer) }
    fun notifyChanged() { main.post { observers.forEach { it() } } }
}
