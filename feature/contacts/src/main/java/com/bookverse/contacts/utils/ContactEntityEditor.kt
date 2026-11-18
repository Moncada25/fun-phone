package com.bookverse.contacts.utils

import com.bookverse.contacts.data.model.Contact
import contacts.core.entities.EmailEntity
import contacts.core.entities.EventEntity
import contacts.core.entities.MutableContact
import contacts.core.util.addEmail
import contacts.core.util.addEvent
import contacts.core.util.addPhone
import contacts.core.util.addWebsite
import contacts.core.util.emails
import contacts.core.util.events
import contacts.core.util.removeAllPhones
import contacts.core.util.removeAllWebsites
import contacts.core.util.setName
import contacts.core.util.setOrganization

internal fun MutableContact.applyEditableFields(contact: Contact) {
    setName { displayName = contact.name }

    removeAllPhones()
    contact.phones.forEach { phone ->
        addPhone {
            number = phone.number
            type = phone.type
        }
    }

    val emailList = emails().toList()
    if (contact.email.isNullOrBlank()) {
        if (emailList.isNotEmpty()) emailList.first().apply { address = null }
    } else if (emailList.isEmpty()) {
        addEmail {
            address = contact.email
            type = EmailEntity.Type.WORK
        }
    } else {
        emailList.first().apply {
            address = contact.email
            type = EmailEntity.Type.WORK
        }
    }

    setOrganization {
        company = contact.company?.takeIf(String::isNotBlank)
    }

    val contactBirthday = contact.birthday
    if (contactBirthday != null) {
        val eventsList = events().toList()
        if (eventsList.isEmpty()) {
            addEvent {
                type = EventEntity.Type.BIRTHDAY
                date = ContactUtils.longToEventDate(contactBirthday)
            }
        } else {
            eventsList.first().apply {
                type = EventEntity.Type.BIRTHDAY
                date = ContactUtils.longToEventDate(contactBirthday)
            }
        }
    }

    removeAllWebsites()
    contact.socialMedia.forEach { url ->
        addWebsite { this.url = url }
    }
}
