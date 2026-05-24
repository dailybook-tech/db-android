package com.laborbook.keep.model

data class OvertimeRequestBody(
    val date: String, // in DD-MM-YYYY format
    val ot_minutes: Double? = null,
    val ot_per_hour: Double? = null,
    val manager_id: String
)
