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
            val displayName = details.callerDisplayName?.trim().orEmpty()
            val repository = QuietRepository(this)
            val decision = runCatching {
                val snapshot = repository.snapshotForScreening(raw)
                val contact = snapshot.preferences.protection && snapshot.preferences.protectContacts &&
                    snapshot.brands.any { it.blocked } && PhoneNumbers.normalize(raw) !in snapshot.allowed && Contacts.contains(this, raw)
                ScreeningEngine.evaluate(raw, snapshot, contact, displayName)
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
                    runCatching {
                        worker.execute {
                            try {
                                var finalDecision = decision
                                var brandId = decision.learnBrandId
                                if (decision.learnBrandId != null && decision.action != Action.ALLOW) {
                                    val learned = repository.learnNumber(decision.learnBrandId, raw)
                                    if (learned == null) {
                                        finalDecision = Decision(decision.action, decision.reason, decision.brand, null)
                                    }
                                }
                                if (brandId == null && decision.brand != null && decision.action != Action.ALLOW) {
                                    brandId = repository.brandIdByName(decision.brand)
                                }
                                repository.record(raw, finalDecision, false)
                                HistoryUpdates.notifyChanged()
                                val prefs = repository.preferences()
                                if (prefs.notifyBlocked && finalDecision.action != Action.ALLOW) {
                                    BlockNotifier.show(
                                        this@QuietCallService,
                                        PhoneNumbers.normalize(raw).ifBlank { raw.ifBlank { "Ẩn số" } },
                                        finalDecision.action,
                                        finalDecision.brand,
                                        brandId,
                                        learned = finalDecision.learnBrandId != null,
                                    )
                                }
                            } catch (_: Exception) {
                            } finally {
                                repository.close()
                            }
                        }
                    }.onFailure { repository.close() }
                } else repository.close()
            }
        }
    }
    override fun onDestroy() { worker.shutdown(); super.onDestroy() }
}
