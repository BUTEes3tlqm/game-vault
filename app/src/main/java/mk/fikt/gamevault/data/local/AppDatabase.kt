package mk.fikt.gamevault.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [GameEntity::class, UserProfileEntity::class, ReviewEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun reviewDao(): ReviewDao

    companion object {
        private const val DB_NAME = "gamevault.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME,
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
