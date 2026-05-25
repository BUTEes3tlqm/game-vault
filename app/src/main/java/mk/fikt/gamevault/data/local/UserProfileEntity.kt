package mk.fikt.gamevault.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val isAnonymous: Boolean = false,
    val joinedAt: Long = System.currentTimeMillis(),
    val fcmToken: String? = null,
    val gameCount: Int = 0,
)
