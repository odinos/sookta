package com.kdev.sookta.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Define the table (Entity)
@Entity(tableName = "user_preferences")
data class UserPreference(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "language") val language: String = "TH",
    @ColumnInfo(name = "user_name") val userName: String? = null,
    @ColumnInfo(name = "age") val age: String? = null,      // เพิ่ม
    @ColumnInfo(name = "gender") val gender: String? = null, // เพิ่ม
    @ColumnInfo(name = "weight") val weight: String? = null, // เพิ่ม
    @ColumnInfo(name = "height") val height: String? = null, // เพิ่ม
    @ColumnInfo(name = "avatar_path") val avatarPath: String? = null,
    @ColumnInfo(name = "is_setup_completed") val isSetupCompleted: Boolean = false
)

@Dao
interface UserPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: UserPreference)

    // อัปเดตภาษา
    @Query("UPDATE user_preferences SET language = :lang WHERE id = 1")
    suspend fun updateLanguage(lang: String)

    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getPreference(): Flow<UserPreference?>

    @Query("UPDATE user_preferences SET user_name = :name, age = :age, gender = :gender, weight = :weight, height = :height WHERE id = 1")
    suspend fun updatePersonalInfo(name: String, age: String, gender: String, weight: String, height: String)

    // อัปเดตรูปและจบการทำงาน (เรียกใช้หน้าสุดท้าย)
    @Query("UPDATE user_preferences SET avatar_path = :avatar, is_setup_completed = 1 WHERE id = 1")
    suspend fun updateAvatarAndFinish(avatar: String)
}

@Database(entities = [UserPreference::class], version = 2, exportSchema = false)
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
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}