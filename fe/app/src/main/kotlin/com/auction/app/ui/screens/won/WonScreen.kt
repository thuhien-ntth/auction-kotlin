package com.auction.app.ui.screens.won

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.auction.app.LocalAppContainer
import com.auction.app.ui.components.AppScaffold
import com.auction.app.ui.navigation.Routes
import com.auction.app.ui.theme.AuctionGold40
import com.auction.app.ui.theme.AuctionGold90

@Composable
fun WonScreen(navController: NavController, onOpenProduct: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: WonViewModel = viewModel(factory = WonViewModel.factory(container.repository))
    val uiState by viewModel.uiState.collectAsState()

    AppScaffold(navController = navController, currentRoute = Routes.WON, title = "Sản phẩm đã thắng") { padding ->
        WonScreenContent(
            uiState = uiState,
            padding = padding,
            onOpenProduct = onOpenProduct
        )
    }
}

@Composable
fun WonScreenContent(
    uiState: WonUiState,
    padding: PaddingValues = PaddingValues(),
    onOpenProduct: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text(
            text = "Danh sách sản phẩm đã thắng",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Text(text = uiState.error ?: "", color = MaterialTheme.colorScheme.error)
        } else if (uiState.auctionStates.isEmpty()) {
            Text("Bạn chưa thắng sản phẩm nào.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.auctionStates) { s ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenProduct(s.productId) },
                        colors = CardDefaults.elevatedCardColors(containerColor = AuctionGold90)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = AuctionGold40,
                                modifier = Modifier.size(40.dp)
                            )
                            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(text = s.title ?: "Sản phẩm ${s.productId.take(8)}", maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(text = "Giá thắng: ${com.auction.app.util.FormatUtils.formatCurrency(s.currentPrice, s.currency)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AuctionGold40)
                                Text(text = "Kết thúc: ${com.auction.app.util.FormatUtils.formatDateTime(s.auctionEndAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
