package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.SudokuMainScreen
import com.example.ui.theme.SudokuTheme
import com.example.viewmodel.SudokuViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SudokuViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            SudokuTheme(themeMode = uiState.settings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SudokuMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
