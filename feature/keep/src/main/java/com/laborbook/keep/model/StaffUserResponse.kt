package com.laborbook.keep.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

data class StaffUserResponseModel(val users: List<StaffUser>)

@Entity(tableName = "staffs")
data class StaffUser(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "mobile_number")
    @SerializedName("mobile_number")
    val mobileNumber: String,
    @ColumnInfo(name = "company_id")
    @SerializedName("company_id")
    val companyId: String,
    @ColumnInfo(name = "user_type")
    @SerializedName("user_type")
    val userType: String,
    @ColumnInfo(name = "category")
    @SerializedName("category")
    val category: String? = null,
    @ColumnInfo(name = "team_id")
    @SerializedName("team_id")
    val teamId: String? = null,
)