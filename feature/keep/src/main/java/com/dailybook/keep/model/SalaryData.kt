package com.dailybook.keep.model

data class SalaryData(
    val total_salary: Double
)

data class CurrentSalaryResponse(
    val salary_per_day: Double,
    val salary_type: String,
    val start_date: String,
    val is_active: Boolean
) 