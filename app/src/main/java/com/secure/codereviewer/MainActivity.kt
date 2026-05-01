package com.secure.codereviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.secure.codereviewer.data.api.AuthManager
import com.secure.codereviewer.ui.screens.CodeReviewerApp
import com.secure.codereviewer.ui.theme.CodeReviewerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize AuthManager with context
        AuthManager.init(applicationContext)

        setContent {
            CodeReviewerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CodeReviewerApp()
                }
            }
        }
    }
}
