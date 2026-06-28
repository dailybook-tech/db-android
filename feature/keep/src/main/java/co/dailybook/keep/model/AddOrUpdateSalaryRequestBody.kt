package co.dailybook.keep.model

data class AddOrUpdateSalaryRequestBody(
    val salary_type: String,
    val salary: Double,
    val start_date: String,
    val bonus: Double? = null
) 