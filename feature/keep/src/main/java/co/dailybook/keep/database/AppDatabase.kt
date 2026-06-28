package co.dailybook.keep.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import co.dailybook.keep.model.AttendanceUser
import co.dailybook.keep.model.CalendarItem
import co.dailybook.keep.model.StaffUser
import co.dailybook.keep.screen.calendar.dao.AttendanceUserDao
import co.dailybook.keep.screen.calendar.dao.CalendarItemDao
import co.dailybook.keep.screen.home.dao.StaffUserDao

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