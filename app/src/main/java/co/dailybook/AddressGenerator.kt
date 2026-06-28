package co.dailybook

import co.dailybook.auth.screen.login.view.LoginActivity
import co.dailybook.base.navigator.ActivitiesNameEnum
import co.dailybook.base.navigator.ActivitiesNameEnum.*
import co.dailybook.base.navigator.ModuleNavigator.*
import co.dailybook.keep.screen.BookKeepActivity


object AddressGenerator {
    fun generateAddressList(): List<ActivityAddress> {
        val list = ArrayList<ActivityAddress>()
        values().forEach {
            val classAddresses = ActivityAddress(it, getClassName(it))
            list.add(classAddresses)
        }
        return list
    }

    private fun getClassName(classNameEnum: ActivitiesNameEnum) = when (classNameEnum) {
        LoginActivityEnum -> LoginActivity::class.java.name
        MainActivityEnum -> MainActivity::class.java.name
        BookKeepActivityEnum -> BookKeepActivity::class.java.name
    }
}