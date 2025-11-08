package com.kdev.sookta.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Define the table (Entity)
@Entity(tableName = "user_preferences")
data class UserPreference(
    @PrimaryKey val id: Int = 1, // Use a fixed ID for single-row preference table
    @ColumnInfo(name = "language") val language: String
)

// 2. Define Data Access Object (DAO)
@Dao
interface UserPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: UserPreference)

    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getPreference(): Flow<UserPreference?>
}


// 3. Define the Database class
@Database(entities = [UserPreference::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userPreferenceDao(): UserPreferenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
