package com.secure.codereviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.secure.codereviewer.util.EditorSetup
import kotlinx.coroutines.launch
import com.secure.codereviewer.data.local.AuthStorage
import com.secure.codereviewer.data.local.HistoryStorage
import com.secure.codereviewer.ui.screens.CodeReviewerApp
import com.secure.codereviewer.ui.theme.CodeReviewerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize storage with context (getSharedPreferences starts loading on a background thread)
        AuthStorage.init(applicationContext)
        HistoryStorage.init(applicationContext)

        // Parse the TextMate grammar/theme on Dispatchers.IO now, so opening the
        // editor later doesn't block the main thread.
        lifecycleScope.launch { EditorSetup.preload(applicationContext) }

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
