package mk.fikt.gamevault.data.remote

object FirestoreSchema {
    const val COLLECTION_REVIEWS = "reviews"
    const val COLLECTION_USERS = "users"

    const val FIELD_ID = "id"
    const val FIELD_GAME_TITLE = "gameTitle"
    const val FIELD_GAME_ID = "gameId"
    const val FIELD_AUTHOR_UID = "authorUid"
    const val FIELD_AUTHOR_NAME = "authorName"
    const val FIELD_RATING = "rating"
    const val FIELD_TEXT = "text"
    const val FIELD_CREATED_AT = "createdAt"

    const val FIELD_FCM_TOKEN = "fcmToken"
    const val FIELD_DISPLAY_NAME = "displayName"
}
