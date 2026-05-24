package mk.fikt.gamevault.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "reviews",
    indices = [Index("authorUid"), Index("gameTitle"), Index("createdAt")]
)
data class ReviewEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val gameTitle: String,
    val gameId: String? = null,
    val authorUid: String,
    val authorName: String,
    val rating: Float = 0f,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val syncedToFirestore: Boolean = false,
)
