package mk.fikt.gamevault.data.messaging

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import mk.fikt.gamevault.R
import mk.fikt.gamevault.data.local.ReviewEntity
import mk.fikt.gamevault.ui.main.MainActivity

/**
 * Fires a local notification when a foreign review arrives for a game the current user has
 * on WISHLIST / BACKLOG. Re-uses the existing [GameVaultMessagingService.CHANNEL_GENERAL].
 */
class ReviewMatchNotifier(private val appContext: Context) {

    fun showReviewMatch(review: ReviewEntity) {
        if (!hasPostNotificationsPermission()) return
        GameVaultMessagingService.ensureChannel(appContext)

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext, review.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val body = appContext.getString(
            R.string.notification_review_match_body,
            review.authorName.ifBlank { appContext.getString(R.string.profile_anonymous) },
            review.gameTitle,
        )

        val notification = NotificationCompat.Builder(appContext, GameVaultMessagingService.CHANNEL_GENERAL)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(appContext.getString(R.string.notification_review_match_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(appContext).notify(review.id.hashCode(), notification)
        }
    }

    private fun hasPostNotificationsPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
