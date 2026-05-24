package com.boilerplate.navigator.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StackItem(val fragmentTag: String, val groupName: String = "", val tabGroup : Int = -1) :
    Parcelable
