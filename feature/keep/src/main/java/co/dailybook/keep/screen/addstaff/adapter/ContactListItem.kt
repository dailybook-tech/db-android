package co.dailybook.keep.screen.addstaff.adapter

import co.dailybook.keep.screen.addstaff.model.ContactItem

/**
 * Sealed class to represent items in the contacts list
 * Can be either a ContactItem or an Ad placeholder
 */
sealed class ContactListItem {
    data class ContactItemView(val contactItem: ContactItem) : ContactListItem()
    data class AdItem(val adPosition: Int) : ContactListItem() // Use position to make each ad unique
}

