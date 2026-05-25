package mk.fikt.gamevault.data.remote

object FirestoreSchema {
    const val COLLECTION_REVIEWS = "reviews"
    const val COLLECTION_USERS = "users"
    const val SUBCOLLECTION_GAMES = "games"

    // Review fields
    const val FIELD_ID = "id"
    const val FIELD_GAME_TITLE = "gameTitle"
    const val FIELD_GAME_ID = "gameId"
    const val FIELD_AUTHOR_UID = "authorUid"
    const val FIELD_AUTHOR_NAME = "authorName"
    const val FIELD_RATING = "rating"
    const val FIELD_TEXT = "text"
    const val FIELD_CREATED_AT = "createdAt"

    // User profile fields
    const val FIELD_DISPLAY_NAME = "displayName"
    const val FIELD_DISPLAY_NAME_LOWER = "displayNameLower"
    const val FIELD_EMAIL = "email"
    const val FIELD_PHOTO_URL = "photoUrl"
    const val FIELD_IS_ANONYMOUS = "isAnonymous"
    const val FIELD_JOINED_AT = "joinedAt"
    const val FIELD_FCM_TOKEN = "fcmToken"
    const val FIELD_GAME_COUNT = "gameCount"

    // Game fields (in users/{uid}/games/{gameId})
    const val FIELD_TITLE = "title"
    const val FIELD_PLATFORM = "platform"
    const val FIELD_RELEASE_YEAR = "releaseYear"
    const val FIELD_GENRE = "genre"
    const val FIELD_STATUS = "status"
    const val FIELD_HOURS_PLAYED = "hoursPlayed"
    const val FIELD_PERSONAL_RATING = "personalRating"
    const val FIELD_PROGRESS_PERCENT = "progressPercent"
    const val FIELD_NOTES = "notes"
    const val FIELD_DATE_ADDED = "dateAdded"
    const val FIELD_DATE_UPDATED = "dateUpdated"
}
