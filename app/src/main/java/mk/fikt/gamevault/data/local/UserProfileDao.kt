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

    @Query("""
        SELECT * FROM user_profiles
        WHERE uid != :excludeUid
          AND isAnonymous = 0
          AND (:query = '' OR LOWER(displayName) LIKE '%' || LOWER(:query) || '%' OR LOWER(email) LIKE '%' || LOWER(:query) || '%')
        ORDER BY displayName COLLATE NOCASE ASC
    """)
    fun observeAllOthers(excludeUid: String, query: String): Flow<List<UserProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(profiles: List<UserProfileEntity>)

    @Query("UPDATE user_profiles SET fcmToken = :token WHERE uid = :uid")
    suspend fun updateFcmToken(uid: String, token: String)

    @Query("UPDATE user_profiles SET displayName = :name, photoUrl = :photoUrl WHERE uid = :uid")
    suspend fun updateNameAndPhoto(uid: String, name: String?, photoUrl: String?)

    @Query("DELETE FROM user_profiles WHERE uid = :uid")
    suspend fun delete(uid: String)
}
