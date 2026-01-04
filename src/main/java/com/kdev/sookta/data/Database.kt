package com.kdev.sookta.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// [Updated] เพิ่ม economic_loss และ body_map_data
@Entity(tableName = "evaluation_history")
data class EvaluationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "activity_name") val activityName: String,
    @ColumnInfo(name = "date_timestamp") val dateTimestamp: Long,
    @ColumnInfo(name = "score_before") val scoreBefore: Double,
    @ColumnInfo(name = "risk_before") val riskBefore: String,
    @ColumnInfo(name = "score_after") val scoreAfter: Double,
    @ColumnInfo(name = "risk_after") val riskAfter: String,
    @ColumnInfo(name = "improvement_note") val improvementNote: String? = null,

    // เพิ่มใหม่
    @ColumnInfo(name = "economic_loss") val economicLoss: Int = 0, // เก็บยอดเงินสูญเสีย (บาท)
    @ColumnInfo(name = "body_map_data") val bodyMapData: String? = null // เก็บ String ของ BodyMap (เช่น "NECK:HIGH,TRUNK:LOW")
)

// ... (Entity UserPreference เหมือนเดิม) ...
@Entity(tableName = "user_preferences")
data class UserPreference(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "language") val language: String = "TH",
    @ColumnInfo(name = "user_name") val userName: String? = null,
    @ColumnInfo(name = "age") val age: String? = null,
    @ColumnInfo(name = "gender") val gender: String? = null,
    @ColumnInfo(name = "weight") val weight: String? = null,
    @ColumnInfo(name = "height") val height: String? = null,
    @ColumnInfo(name = "income_per_year") val incomePerYear: String? = null,
    @ColumnInfo(name = "avatar_path") val avatarPath: String? = null,
    @ColumnInfo(name = "is_setup_completed") val isSetupCompleted: Boolean = false
)

@Dao
interface EvaluationDao {
    @Insert
    suspend fun insertEvaluation(evaluation: EvaluationEntity)

    @Query("SELECT * FROM evaluation_history ORDER BY date_timestamp DESC")
    suspend fun getAllHistory(): List<EvaluationEntity>

    @Query("SELECT * FROM evaluation_history WHERE id = :id LIMIT 1")
    suspend fun getEvaluationById(id: Int): EvaluationEntity?
}

@Dao
interface UserPreferenceDao {
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(userPreference: UserPreference)

    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getPreference(): Flow<UserPreference?>

    @Query("UPDATE user_preferences SET language = :lang WHERE id = 1")
    suspend fun updateLanguage(lang: String)

    @Query("UPDATE user_preferences SET user_name = :name, age = :age, gender = :gender, weight = :weight, height = :height, income_per_year = :income WHERE id = 1")
    suspend fun updatePersonalInfo(name: String, age: String, gender: String, weight: String, height: String, income: String)

    @Query("UPDATE user_preferences SET avatar_path = :path, is_setup_completed = 1 WHERE id = 1")
    suspend fun updateAvatarAndFinish(path: String)
}

// [Updated] เปลี่ยน version เป็น 5
@Database(entities = [UserPreference::class, EvaluationEntity::class], version = 5, exportSchema = false)
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
                    "sookta_database"
                )
                    .fallbackToDestructiveMigration(false) // อนุญาตให้ล้างข้อมูลเก่าเมื่อเปลี่ยนโครงสร้าง DB
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}