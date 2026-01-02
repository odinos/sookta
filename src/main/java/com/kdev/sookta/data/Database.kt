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

@Entity(tableName = "evaluation_history")
data class EvaluationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val activityName: String,
    val dateTimestamp: Long,
    val scoreBefore: Double,
    val riskBefore: String, // e.g. "HIGH", "LOW"
    val scoreAfter: Double,
    val riskAfter: String,
    val improvementNote: String // บันทึกว่าแก้ไขอะไรไปบ้าง (Optional)
)

@Dao
interface EvaluationDao {
    @Insert
    suspend fun insertEvaluation(evaluation: EvaluationEntity)

    @Query("SELECT * FROM evaluation_history ORDER BY dateTimestamp DESC")
    suspend fun getAllHistory(): List<EvaluationEntity>
}

@Dao
interface UserPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: UserPreference)

    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getPreference(): Flow<UserPreference?>

    // อัปเดตภาษา
    @Query("UPDATE user_preferences SET language = :lang WHERE id = 1")
    suspend fun updateLanguage(lang: String)



    // 1. ใช้ในหน้า SetupScreen (กรอกข้อมูล แต่ยังไม่จบ เพราะต้องไปเลือกรูปต่อ)
    @Query("UPDATE user_preferences SET user_name = :name, age = :age, gender = :gender, weight = :weight, height = :height WHERE id = 1")
    suspend fun updatePersonalInfo(name: String, age: String, gender: String, weight: String, height: String)

    // 2. ใช้ในหน้า AvatarSelectionScreen (บันทึกรูป และ จบการทำงาน)
    @Query("UPDATE user_preferences SET avatar_path = :avatar, is_setup_completed = 1 WHERE id = 1")
    suspend fun updateAvatarAndFinish(avatar: String)
}

@Database(entities = [UserPreference::class, EvaluationEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun evaluationDao(): EvaluationDao
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