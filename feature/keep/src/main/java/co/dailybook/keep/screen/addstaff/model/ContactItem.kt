package co.dailybook.keep.screen.addstaff.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactItem(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val name: String,
    val mobileNumber: String
)