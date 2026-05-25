package mk.fikt.gamevault.data.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mk.fikt.gamevault.R
import mk.fikt.gamevault.data.remote.FirestoreSchema
import mk.fikt.gamevault.di.AppContainer
import mk.fikt.gamevault.ui.main.MainActivity

class GameVaultMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = AppContainer.authRepository.currentUser()?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                AppContainer.database.userProfileDao().updateFcmToken(uid, token)
            }
            if (AppContainer.firebaseAvailable) {
                runCatching {
                    FirebaseFirestore.getInstance()
                        .collection(FirestoreSchema.COLLECTION_USERS)
                        .document(uid)
                        .set(
                            mapOf(FirestoreSchema.FIELD_FCM_TOKEN to token),
                            com.google.firebase.firestore.SetOptions.merge(),
                        )
                }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.app_name)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService<NotificationManager>() ?: return
        ensureChannel(manager)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_GENERAL)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (manager.getNotificationChannel(CHANNEL_GENERAL) != null) return
        val channel = NotificationChannel(
            CHANNEL_GENERAL,
            getString(R.string.notification_channel_general_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.notification_channel_general_description)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_GENERAL = "general"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService<NotificationManager>() ?: return
            if (manager.getNotificationChannel(CHANNEL_GENERAL) != null) return
            val channel = NotificationChannel(
                CHANNEL_GENERAL,
                context.getString(R.string.notification_channel_general_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_general_description)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
