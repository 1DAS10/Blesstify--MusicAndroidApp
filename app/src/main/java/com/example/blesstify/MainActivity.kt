package com.example.blesstify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.blesstify.presentation.ui.navigation.BlessifyNavGraph
import com.example.blesstify.presentation.ui.theme.BlessifyTheme
import com.example.blesstify.presentation.ui.theme.ImageLoaderProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImageLoaderProvider {
                BlessifyTheme {
                    BlessifyNavGraph()
                }
            }
        }
    }
}
