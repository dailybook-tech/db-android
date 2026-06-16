package com.dailybook.base.navigator

import android.content.Context
import android.content.Intent

class ModuleNavigator(
    private val classAddresses: List<ActivityAddress>
) {
    fun startActivity(context: Context, classNameEnum: ActivitiesNameEnum) {
        val address = classAddresses.first { it.requestedActivity == classNameEnum }.activityAddress
        try {
            val intent = Intent(
                context,
                Class.forName(address)
            )
            context.startActivity(intent)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
    }

    data class ActivityAddress(
        val requestedActivity: ActivitiesNameEnum,
        val activityAddress: String
    )
}

enum class ActivitiesNameEnum {
    LoginActivityEnum,
    MainActivityEnum,
    BookKeepActivityEnum
}