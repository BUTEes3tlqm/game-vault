package mk.fikt.gamevault.data.messaging

import com.google.firebase.messaging.FirebaseMessaging

object PushSubscriptions {
    const val TOPIC_ALL_USERS = "all-users"

    fun subscribeToBroadcast(): Boolean = runCatching {
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_ALL_USERS)
        true
    }.getOrDefault(false)

    fun unsubscribeFromBroadcast() {
        runCatching { FirebaseMessaging.getInstance().unsubscribeFromTopic(TOPIC_ALL_USERS) }
    }
}
