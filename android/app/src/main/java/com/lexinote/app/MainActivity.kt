package com.lexinote.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lexinote.app.ui.LexiNoteApp
import com.lexinote.app.ui.theme.LexiNoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LexiNoteTheme {
                LexiNoteApp()
            }
        }
    }
}
