package com.boilerplate.navigator

import com.boilerplate.navigator.transaction.NavigatorTransaction


data class NavigatorConfiguration(val initialTabIndex: Int = 0,
                                  val alwaysExitFromInitial: Boolean = false,
                                  val defaultNavigatorTransaction: NavigatorTransaction = NavigatorTransaction.ATTACH_DETACH)