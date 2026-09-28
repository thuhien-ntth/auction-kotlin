package com.auction.app.ui.screens.exhibition

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.auction.app.LocalAppContainer
import com.auction.app.network.model.ProductSummary
import com.auction.app.ui.components.AppScaffold
import com.auction.app.ui.components.ProductThumbnail
import com.auction.app.ui.components.StatusChip
import com.auction.app.ui.navigation.Routes
import com.auction.app.ui.theme.AuctionAppTheme
import java.math.BigDecimal

@Composable
fun ExhibitionManagementScreen(navController: NavController, onOpenProduct: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ExhibitionManagementViewModel = viewModel(factory = ExhibitionManagementViewModel.factory(container.repository))
    val uiState by viewModel.uiState.collectAsState()

    AppScaffold(navController = navController, currentRoute = Routes.EXHIBITION_MANAGEMENT, title = "Sản phẩm đã đăng") { padding ->
        ExhibitionManagementScreenContent(
            uiState = uiState,
            padding = padding,
            onOpenProduct = onOpenProduct
        )
    }
}

@Composable
fun ExhibitionManagementScreenContent(
    uiState: ExhibitionManagementUiState,
    padding: PaddingValues = PaddingValues(),
    onOpenProduct: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Text(text = uiState.error ?: "", color = MaterialTheme.colorScheme.error)
        } else if (uiState.products.isEmpty()) {
            Text(
                "Bạn chưa đăng sản phẩm nào. Vào mục \"Đăng SP\" để đăng sản phẩm mới.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.products) { p ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenProduct(p.id) }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProductThumbnail(imageUrl = p.imageUrl)
                            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(text = p.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(text = "Danh mục: ${p.category}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "Giá khởi điểm: ${com.auction.app.util.FormatUtils.formatCurrency(p.startPrice, p.currency)}", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.padding(2.dp))
                                StatusChip(status = p.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExhibitionManagementScreenPreview() {
    AuctionAppTheme {
        ExhibitionManagementScreenContent(
            uiState = ExhibitionManagementUiState(
                isLoading = false,
                products = listOf(
                    ProductSummary(
                        id = "prod-5", title = "Bộ bàn ghế Trắc Cẩm 6 món", category = "ANTIQUES",
                        startPrice = BigDecimal("120000000"), status = "PENDING_APPROVAL",
                        auctionStartAt = null, auctionEndAt = null, imageUrl = null
                    ),
                    ProductSummary(
                        id = "prod-6", title = "MacBook Pro 16 M3 Max", category = "ELECTRONICS",
                        startPrice = BigDecimal("72000000"), status = "REJECTED",
                        auctionStartAt = null, auctionEndAt = null, imageUrl = null
                    )
                )
            ),
            onOpenProduct = {}
        )
    }
}
