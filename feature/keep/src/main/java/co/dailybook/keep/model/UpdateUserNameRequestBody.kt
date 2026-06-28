package co.dailybook.keep.model

data class UpdateUserNameRequestBody(
    val name: String,
    val mobile_number: String? = null
)