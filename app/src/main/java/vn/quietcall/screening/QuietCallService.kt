package vn.quietcall.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import android.os.Handler
import android.os.Looper
import vn.quietcall.data.QuietRepository
import vn.quietcall.data.HistoryUpdates
import vn.quietcall.domain.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Respond before Android's 5-second deadline; slow storage/contacts fail open. */
class QuietCallService : CallScreeningService() {
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    override fun onScreenCall(details: Call.Details) {
        if (details.callDirection != Call.Details.DIRECTION_INCOMING) return
        val replied = AtomicBoolean(false)
        val timeout = Runnable {
            if (replied.compareAndSet(false, true)) respondToCall(details, CallResponse.Builder().build())
        }
        main.postDelayed(timeout, 3500)
        worker.execute {
            val raw = details.handle?.takeIf { it.scheme == "tel" }?.schemeSpecificPart.orEmpty()
            val repository = QuietRepository(this)
            val decision = runCatching {
                val snapshot = repository.snapshotForNumber(raw)
                val contact = snapshot.preferences.protection && snapshot.preferences.protectContacts &&
                    snapshot.brands.any { it.blocked } && PhoneNumbers.normalize(raw) !in snapshot.allowed && Contacts.contains(this, raw)
                ScreeningEngine.evaluate(raw, snapshot, contact)
            }.getOrElse { Decision(Action.ALLOW, "Không thể đọc dữ liệu; cho phép để tránh chặn nhầm") }
            main.post {
                if (replied.compareAndSet(false, true)) {
                    main.removeCallbacks(timeout)
                    val response = CallResponse.Builder().apply {
                        when (decision.action) {
                            Action.BLOCK -> { setDisallowCall(true); setRejectCall(true); setSkipNotification(true); setSkipCallLog(false) }
                            Action.SILENCE -> setSilenceCall(true)
                            Action.ALLOW -> Unit
                        }
                    }.build()
                    respondToCall(details, response)
                    // The OS may unbind immediately after the response. Never crash if shutdown raced this log.
                    runCatching {
                        worker.execute {
                            try {
                                repository.record(raw, decision, false)
                                HistoryUpdates.notifyChanged()
                            } catch (_: Exception) {} finally { repository.close() }
                        }
                    }.onFailure { repository.close() }
                } else repository.close()
            }
        }
    }
    override fun onDestroy() { worker.shutdown(); super.onDestroy() }
}
