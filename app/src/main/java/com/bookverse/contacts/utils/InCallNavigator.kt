package com.bookverse.contacts.utils

import android.content.Context
import android.content.Intent
import com.bookverse.contacts.MainActivity

object InCallNavigator {
    fun open(context: Context, number: String) {
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.putExtra("open_incall", true)
        intent.putExtra("incall_number", number)
        context.startActivity(intent)
    }
}

