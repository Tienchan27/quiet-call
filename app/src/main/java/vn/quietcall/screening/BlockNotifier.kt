package vn.quietcall.screening

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import vn.quietcall.MainActivity
import vn.quietcall.R
import vn.quietcall.domain.Action

object BlockNotifier {
    const val CHANNEL_ID = "blocked_calls"
    private const val CHANNEL_NAME = "Cuộc gọi đã chặn"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Thông báo khi QuietCall chặn hoặc tắt chuông cuộc gọi"
            },
        )
    }

    fun show(
        context: Context,
        number: String,
        action: Action,
        brandName: String?,
        brandId: Long?,
        learned: Boolean,
    ) {
        ensureChannel(context)
        val app = context.applicationContext
        val manager = app.getSystemService(NotificationManager::class.java) ?: return
        val title = when (action) {
            Action.SILENCE -> "Đã tắt chuông"
            else -> "Đã chặn cuộc gọi"
        }
        val brandPart = brandName?.let { " · $it" }.orEmpty()
        val body = if (learned) {
            "$number$brandPart — đã thêm số vào nhóm. Cho phép hoặc gỡ nếu cần nghe."
        } else {
            "$number$brandPart"
        }
        val notificationId = (number.hashCode() xor (brandId ?: 0L).toInt()) and 0x7fffffff
        val openApp = PendingIntent.getActivity(
            app,
            notificationId,
            Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val allow = PendingIntent.getBroadcast(
            app,
            notificationId + 1,
            Intent(app, BlockActionReceiver::class.java).setAction(BlockActionReceiver.ACTION_ALLOW)
                .putExtra(BlockActionReceiver.EXTRA_NUMBER, number)
                .putExtra(BlockActionReceiver.EXTRA_NOTIFICATION_ID, notificationId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = Notification.Builder(app, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .addAction(Notification.Action.Builder(null, "Cho phép số này", allow).build())
        if (brandId != null && brandId > 0) {
            val remove = PendingIntent.getBroadcast(
                app,
                notificationId + 2,
                Intent(app, BlockActionReceiver::class.java).setAction(BlockActionReceiver.ACTION_REMOVE)
                    .putExtra(BlockActionReceiver.EXTRA_NUMBER, number)
                    .putExtra(BlockActionReceiver.EXTRA_BRAND_ID, brandId)
                    .putExtra(BlockActionReceiver.EXTRA_NOTIFICATION_ID, notificationId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(Notification.Action.Builder(null, "Gỡ khỏi nhóm", remove).build())
        }
        runCatching { manager.notify(notificationId, builder.build()) }
    }
}
