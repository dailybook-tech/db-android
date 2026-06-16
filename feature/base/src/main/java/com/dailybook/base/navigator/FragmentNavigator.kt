package com.dailybook.base.navigator

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.boilerplate.navigator.MultipleStackNavigator
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FragmentNavigator {
    lateinit var multipleStackNavigator: MultipleStackNavigator

    fun initialize(iMultipleStackNavigator: MultipleStackNavigator, savedState: Bundle?) {
        this.multipleStackNavigator = iMultipleStackNavigator
        this.multipleStackNavigator?.initialize(savedState)
    }

    fun start(fragment: Fragment, groupName: String) {
        multipleStackNavigator?.start(fragment, groupName)
    }

    fun start(fragment: Fragment) {
        multipleStackNavigator?.start(fragment)
    }

    fun start(bottomSheetFragment: BottomSheetDialogFragment) {
        multipleStackNavigator?.start(bottomSheetFragment)
    }

    fun canGoBack(): Boolean? {
        return multipleStackNavigator?.canGoBack()
    }

    fun goBack() {
        multipleStackNavigator?.goBack()
    }

    fun onSaveInstanceState(outState: Bundle) {
        multipleStackNavigator?.onSaveInstanceState(outState)
    }

    fun switchTab(tabPosition: Int) {
        multipleStackNavigator?.switchTab(tabPosition)
    }
}