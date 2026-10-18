package vn.quietcall.screening

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import vn.quietcall.data.HistoryUpdates
import vn.quietcall.data.QuietRepository
import vn.quietcall.domain.PhoneNumbers

class BlockActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val number = intent.getStringExtra(EXTRA_NUMBER).orEmpty()
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val app = context.applicationContext
        val repository = QuietRepository(app)
        try {
            when (intent.action) {
                ACTION_ALLOW -> {
                    if (!PhoneNumbers.valid(number)) {
                        Toast.makeText(app, "Không có số hợp lệ.", Toast.LENGTH_SHORT).show()
                        return
                    }
                    repository.addAllowed(number)
                    Toast.makeText(app, "Đã cho phép $number.", Toast.LENGTH_SHORT).show()
                    HistoryUpdates.notifyChanged()
                }
                ACTION_REMOVE -> {
                    val brandId = intent.getLongExtra(EXTRA_BRAND_ID, -1L)
                    if (brandId <= 0 || !PhoneNumbers.valid(number)) {
                        Toast.makeText(app, "Không gỡ được số.", Toast.LENGTH_SHORT).show()
                        return
                    }
                    val removed = repository.removeNumberFromBrand(brandId, number)
                    Toast.makeText(
                        app,
                        if (removed) "Đã gỡ $number khỏi nhóm." else "Số không còn trong nhóm.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    if (removed) HistoryUpdates.notifyChanged()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(app, e.message ?: "Không thể cập nhật.", Toast.LENGTH_SHORT).show()
        } finally {
            repository.close()
            if (notificationId >= 0) {
                app.getSystemService(NotificationManager::class.java)?.cancel(notificationId)
            }
        }
    }

    companion object {
        const val ACTION_ALLOW = "vn.quietcall.action.ALLOW_NUMBER"
        const val ACTION_REMOVE = "vn.quietcall.action.REMOVE_NUMBER"
        const val EXTRA_NUMBER = "number"
        const val EXTRA_BRAND_ID = "brand_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
