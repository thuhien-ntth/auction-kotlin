package com.auction.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Toast dùng chung toàn app: hiện ở góc trên bên phải, bấm dấu X để tắt, tự tắt sau 5 giây.
 * Dùng: val toast = LocalToastState.current; toast.show("..."); toast.show("...", isError = false)
 */
class ToastState {
    var message by mutableStateOf<String?>(null)
        private set
    var isError by mutableStateOf(true)
        private set
    // Tăng mỗi lần show -> cùng 1 nội dung hiện lại lần 2 vẫn reset đếm 5 giây
    var id by mutableStateOf(0L)
        private set

    fun show(msg: String, isError: Boolean = true) {
        this.message = msg
        this.isError = isError
        id++
    }

    fun dismiss() {
        message = null
    }
}

val LocalToastState = compositionLocalOf { ToastState() }

@Composable
fun TopRightToast(state: ToastState, modifier: Modifier = Modifier) {
    val message = state.message
    LaunchedEffect(state.id) {
        if (state.message != null) {
            delay(5000)
            state.dismiss()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = message != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).padding(top = 32.dp)
        ) {
            if (message != null) {
                Row(
                    modifier = Modifier
                        .background(
                            if (state.isError) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .widthIn(max = 300.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message,
                        color = Color.White,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = Color.White,
                        modifier = Modifier.clickable { state.dismiss() }.size(20.dp)
                    )
                }
            }
        }
    }
}
