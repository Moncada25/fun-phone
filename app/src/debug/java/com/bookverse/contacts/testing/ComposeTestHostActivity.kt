package com.bookverse.contacts.testing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ComposeTestContent {
    private val placeholder: @Composable () -> Unit = { Text("compose-host-ready") }
    private var content by mutableStateOf(placeholder)

    fun show(newContent: @Composable () -> Unit) {
        content = newContent
    }

    fun reset() {
        content = placeholder
    }

    @Composable
    fun Render() {
        content()
    }
}

class ComposeTestHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeTestContent.Render()
        }
    }

    override fun onDestroy() {
        ComposeTestContent.reset()
        super.onDestroy()
    }
}
