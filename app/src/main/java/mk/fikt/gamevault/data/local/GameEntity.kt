package mk.fikt.gamevault.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import mk.fikt.gamevault.data.model.GamePlatform
import mk.fikt.gamevault.data.model.GameStatus
import java.util.UUID

@Entity(
    tableName = "games",
    indices = [Index("ownerUid"), Index("status"), Index("title")]
)
data class GameEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ownerUid: String,
    val title: String,
    val platform: GamePlatform = GamePlatform.OTHER,
    val releaseYear: Int? = null,
    val genre: String? = null,
    val coverUri: String? = null,
    val status: GameStatus = GameStatus.BACKLOG,
    val hoursPlayed: Double = 0.0,
    val personalRating: Float = 0f,
    val progressPercent: Int = 0,
    val notes: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val dateUpdated: Long = System.currentTimeMillis(),
    val syncedToFirestore: Boolean = false,
)
