package com.dailybook.keep.screen.home.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dailybook.keep.model.StaffUser

@Dao
interface StaffUserDao {
    @Query("SELECT * FROM staffs")
    suspend fun getAllStaffUsers(): List<StaffUser>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaffUsers(users: List<StaffUser>)

    @Query("DELETE FROM staffs")
    suspend fun deleteAllStaffs()
}