package com.auction.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import com.auction.app.ui.navigation.AppNavHost
import com.auction.app.ui.theme.AuctionAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as AuctionApplication).container

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                AuctionAppTheme {
                    val focusManager = LocalFocusManager.current
                    val toastState = androidx.compose.runtime.remember { com.auction.app.ui.components.ToastState() }
                    
                    CompositionLocalProvider(com.auction.app.ui.components.LocalToastState provides toastState) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = { focusManager.clearFocus() })
                                },
                            color = MaterialTheme.colorScheme.background
                        ) {
                            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                                AppNavHost()
                                com.auction.app.ui.components.TopRightToast(state = toastState)
                            }
                        }
                    }
                }
            }
        }
    }
}

