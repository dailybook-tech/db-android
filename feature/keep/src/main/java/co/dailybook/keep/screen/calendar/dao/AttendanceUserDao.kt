package co.dailybook.keep.screen.calendar.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy
import co.dailybook.keep.model.AttendanceUser

@Dao
interface AttendanceUserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: AttendanceUser)

    @Query("SELECT * FROM attendance_user WHERE id = :id")
    suspend fun getUserById(id: String): AttendanceUser

    @Query("DELETE FROM attendance_user WHERE id = :id")
    suspend fun deleteByUserId(id: String)

    @Query("DELETE FROM attendance_user")
    suspend fun deleteAll()
}