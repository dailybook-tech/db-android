package com.dailybook.keep.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.dailybook.keep.model.AttendanceUser
import com.dailybook.keep.model.CalendarItem
import com.dailybook.keep.model.StaffUser
import com.dailybook.keep.screen.calendar.dao.AttendanceUserDao
import com.dailybook.keep.screen.calendar.dao.CalendarItemDao
import com.dailybook.keep.screen.home.dao.StaffUserDao

@Database(entities = [StaffUser::class, AttendanceUser::class, CalendarItem::class], version = 7)
abstract class AppDatabase : RoomDatabase() {
    abstract fun staffUserDao(): StaffUserDao
    abstract fun attendanceUserDao(): AttendanceUserDao
    abstract fun calendarItemDao(): CalendarItemDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}