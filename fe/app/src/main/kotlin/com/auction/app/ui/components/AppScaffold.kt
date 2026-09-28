package com.auction.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import com.auction.app.LocalAppContainer
import com.auction.app.ui.navigation.Routes
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Balance
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auction.app.ui.theme.Navy

import androidx.compose.material.icons.filled.Shield
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavController,
    currentRoute: String,
    title: String,
    content: @Composable (PaddingValues) -> Unit
) {
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current
    val scope = rememberCoroutineScope()

    var container: com.auction.app.AppContainer? = null
    if (!isPreview) {
        container = LocalAppContainer.current
    }
    
    val isAdmin = if (isPreview) false else {
        val state by container!!.tokenStore.isAdminFlow.collectAsState(initial = false)
        state
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Balance,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 6.dp)
                        )
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 1.sp
                        )
                    }
                },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        androidx.compose.material3.IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Routes.PRODUCT_SEARCH,
                    onClick = { navController.navigate(Routes.PRODUCT_SEARCH) { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Tìm\nkiếm", fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 12.sp) }
                )
                // Admin chỉ thấy: Tìm kiếm (tất cả sản phẩm) + Duyệt sản phẩm + Đăng xuất
                if (!isAdmin) {
                    NavigationBarItem(
                        selected = currentRoute == Routes.EXHIBITION_MANAGEMENT,
                        onClick = { navController.navigate(Routes.EXHIBITION_MANAGEMENT) { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                        label = { Text("Sản phẩm\ncủa tôi", fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 12.sp) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.PRODUCT_REGISTER,
                        onClick = { navController.navigate(Routes.PRODUCT_REGISTER) { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.AddBox, contentDescription = null) },
                        label = { Text("Đăng sản\nphẩm", fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 12.sp) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.PARTICIPATING,
                        onClick = { navController.navigate(Routes.PARTICIPATING) { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Gavel, contentDescription = null) },
                        label = { Text("Đang\ntham gia", fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 12.sp) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.WON,
                        onClick = { navController.navigate(Routes.WON) { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null) },
                        label = { Text("Đã\nthắng", fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 12.sp) }
                    )
                }
                if (isAdmin) {
                    NavigationBarItem(
                        selected = currentRoute == Routes.ADMIN_DASHBOARD,
                        onClick = { navController.navigate(Routes.ADMIN_DASHBOARD) { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Shield, contentDescription = null) },
                        label = { Text("Duyệt\nsản phẩm", fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 12.sp) }
                    )
                }
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        scope.launch {
                            runCatching { container?.api?.logout() }
                            container?.tokenStore?.clear()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.Logout, contentDescription = null) },
                    label = { Text("Đăng\nxuất", fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 12.sp) }
                )
            }
        }
    ) { padding ->
        content(padding)
    }
}
