package com.example.noadse_reader

import android.content.Context
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val fileName: String,
    val title: String,
    val author: String,
    val sizeBytes: Long,
    val addedAt: Long,
    val format: String,
    val lastLocator: String? = null,
    val coverPath: String? = null,
    val progress: Double? = null
)

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT fileName FROM books")
    suspend fun allFileNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(book: BookEntity)

    @Query("SELECT lastLocator FROM books WHERE fileName = :fileName")
    suspend fun getLocator(fileName: String): String?

    @Query(
        "UPDATE books SET lastLocator = :locator, " +
                "progress = COALESCE(:progress, progress) WHERE fileName = :fileName"
    )
    suspend fun saveLocator(fileName: String, locator: String, progress: Double?)

    @Query("SELECT * FROM books WHERE coverPath IS NULL")
    suspend fun booksNeedingCovers(): List<BookEntity>

    @Query("UPDATE books SET coverPath = :path WHERE fileName = :fileName")
    suspend fun setCover(fileName: String, path: String)

    @Query("DELETE FROM books WHERE fileName = :fileName")
    suspend fun delete(fileName: String)
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE books ADD COLUMN lastLocator TEXT").use { it.step() }
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE books ADD COLUMN coverPath TEXT").use { it.step() }
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE books ADD COLUMN progress REAL").use { it.step() }
    }
}

@Database(entities = [BookEntity::class], version = 4, exportSchema = false)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun books(): BookDao

    companion object {
        @Volatile
        private var instance: LibraryDatabase? = null

        fun get(context: Context): LibraryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder<LibraryDatabase>(
                    context.applicationContext,
                    "library.db"
                )
                    .setDriver(AndroidSQLiteDriver())
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}