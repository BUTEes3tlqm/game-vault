package mk.fikt.gamevault.data.local

import androidx.room.TypeConverter
import mk.fikt.gamevault.data.model.GamePlatform
import mk.fikt.gamevault.data.model.GameStatus

class Converters {
    @TypeConverter
    fun gameStatusToString(status: GameStatus): String = status.name

    @TypeConverter
    fun stringToGameStatus(value: String?): GameStatus = GameStatus.fromName(value)

    @TypeConverter
    fun gamePlatformToString(platform: GamePlatform): String = platform.name

    @TypeConverter
    fun stringToGamePlatform(value: String?): GamePlatform = GamePlatform.fromName(value)
}
