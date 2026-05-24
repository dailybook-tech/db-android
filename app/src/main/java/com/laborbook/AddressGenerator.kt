package com.laborbook

import com.laborbook.auth.screen.login.view.LoginActivity
import com.laborbook.base.navigator.ActivitiesNameEnum
import com.laborbook.base.navigator.ActivitiesNameEnum.*
import com.laborbook.base.navigator.ModuleNavigator.*
import com.laborbook.keep.screen.BookKeepActivity


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