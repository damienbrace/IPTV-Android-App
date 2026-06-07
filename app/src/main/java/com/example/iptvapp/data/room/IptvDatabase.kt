package com.example.iptvapp.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        ChannelEntity::class,
        EpgProgramEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class IptvDatabase : RoomDatabase() {
    abstract fun iptvDao(): IptvDao

    companion object {
        @Volatile
        private var instance: IptvDatabase? = null

        fun getInstance(context: Context): IptvDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    IptvDatabase::class.java,
                    "streamhub.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE channels ADD COLUMN streamId INTEGER")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE channels ADD COLUMN streamKind TEXT NOT NULL DEFAULT 'live'")
                db.execSQL("ALTER TABLE channels ADD COLUMN containerExtension TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE channels ADD COLUMN epgChannelId TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_channels_epgChannelId ON channels(epgChannelId)")
            }
        }
    }
}
