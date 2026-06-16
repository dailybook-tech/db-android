package com.dailybook.keep.screen.calendar.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy
import com.dailybook.keep.model.CalendarItem

@Dao
interface CalendarItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CalendarItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CalendarItem>)

    @Query("SELECT * FROM calendar_item WHERE id = :id AND month = :month AND year = :year")
    suspend fun getAllByIdMonthYear(id: String, month: String, year: String): List<CalendarItem>

    @Query("DELETE FROM calendar_item WHERE id = :id AND month = :month AND year = :year")
    suspend fun deleteByUserIdMonthYear(id: String, month: String, year: String)

    @Query("SELECT COUNT(*) FROM calendar_item")
    suspend fun getCount(): Int

    @Query("DELETE FROM calendar_item WHERE id IN (SELECT id FROM calendar_item ORDER BY id ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)

    @Query("DELETE FROM calendar_item")
    suspend fun deleteAll()
}