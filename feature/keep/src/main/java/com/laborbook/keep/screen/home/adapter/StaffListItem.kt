package com.laborbook.keep.screen.home.adapter

import com.laborbook.keep.model.StaffUser

/**
 * Sealed class to represent items in the staff list
 * Can be either a StaffUser or an Ad placeholder
 */
sealed class StaffListItem {
    data class StaffItem(val staffUser: StaffUser, val isLocked: Boolean = false) : StaffListItem()
    data class AdItem(val adPosition: Int) : StaffListItem() // Use position to make each ad unique
}

