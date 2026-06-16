package com.dailybook.keep.screen.addstaff.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts")
    suspend fun getAllContacts(): List<ContactItem>

    @Insert
    suspend fun insertContacts(vararg contacts: ContactItem)

    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()
}