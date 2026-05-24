package mk.fikt.gamevault.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profiles WHERE uid = :uid")
    fun observe(uid: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE uid = :uid")
    suspend fun get(uid: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Query("UPDATE user_profiles SET fcmToken = :token WHERE uid = :uid")
    suspend fun updateFcmToken(uid: String, token: String)

    @Query("DELETE FROM user_profiles WHERE uid = :uid")
    suspend fun delete(uid: String)
}
