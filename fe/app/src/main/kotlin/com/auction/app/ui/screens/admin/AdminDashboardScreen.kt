package com.auction.app.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun AdminDashboardScreen(
    navController: NavController,
    onOpenProduct: (String) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: AdminDashboardViewModel = viewModel(factory = AdminDashboardViewModel.factory(container.repository))
    val uiState by viewModel.uiState.collectAsState()

    val toastState = com.auction.app.ui.components.LocalToastState.current

    LaunchedEffect(uiState.actionMessage, uiState.error) {
        uiState.actionMessage?.let {
            toastState.show(it, isError = false)
            viewModel.clearActionMessage()
        }
        uiState.error?.let {
            toastState.show(it)
            viewModel.clearActionMessage()
        }
    }

    AppScaffold(
        navController = navController,
        currentRoute = Routes.ADMIN_DASHBOARD,
        title = "Duyệt sản phẩm"
    ) { padding ->
        Scaffold(
            modifier = Modifier.padding(padding)
        ) { innerPadding ->
            AdminDashboardContent(
                uiState = uiState,
                padding = innerPadding,
                onOpenProduct = onOpenProduct,
                onApprove = { viewModel.approveProduct(it) },
                onReject = { id, reason -> viewModel.rejectProduct(id, reason) },
                onSelectTab = { viewModel.selectTab(it) }
            )

        }
    }
}

@Composable
fun AdminDashboardContent(
    uiState: AdminDashboardUiState,
    padding: PaddingValues = PaddingValues(),
    onOpenProduct: (String) -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String, String) -> Unit,
    onSelectTab: (AdminTab) -> Unit = {}
) {
    var rejectingId by remember { mutableStateOf<String?>(null) }
    var reason by remember { mutableStateOf("") }

    rejectingId?.let { id ->
        AlertDialog(
            onDismissRequest = { rejectingId = null },
            title = { Text("Lý do từ chối") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Nhập lý do (bắt buộc)") },
                    minLines = 2
                )
            },
            confirmButton = {
                TextButton(
                    enabled = reason.isNotBlank(),
                    onClick = {
                        onReject(id, reason.trim())
                        rejectingId = null
                    }
                ) { Text("Từ chối") }
            },
            dismissButton = {
                TextButton(onClick = { rejectingId = null }) { Text("Hủy") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
    ) {
        val products = when (uiState.selectedTab) {
            AdminTab.PENDING -> uiState.pendingProducts
            AdminTab.APPROVED -> uiState.approvedProducts
            AdminTab.REJECTED -> uiState.rejectedProducts
        }
        val emptyText = when (uiState.selectedTab) {
            AdminTab.PENDING -> "Không có sản phẩm nào đang chờ duyệt."
            AdminTab.APPROVED -> "Chưa có sản phẩm nào được duyệt."
            AdminTab.REJECTED -> "Chưa có sản phẩm nào bị từ chối."
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (products.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(products, key = { it.id }) { p ->
                    if (uiState.selectedTab == AdminTab.PENDING) {
                        PendingProductCard(
                            product = p,
                            onOpen = { onOpenProduct(p.id) },
                            onApprove = { onApprove(p.id) },
                            onReject = {
                                reason = ""
                                rejectingId = p.id
                            }
                        )
                    } else {
                        ReviewedProductCard(product = p, onOpen = { onOpenProduct(p.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewedProductCard(
    product: ProductSummary,
    onOpen: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductThumbnail(imageUrl = product.imageUrl)
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Danh mục: ${product.category}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Giá khởi điểm: ${com.auction.app.util.FormatUtils.formatCurrency(product.startPrice, product.currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusChip(status = product.status)
                }
            }
            if (product.status == "REJECTED") {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Lý do từ chối: ${product.rejectionReason?.takeIf { it.isNotBlank() } ?: "(không có)"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PendingProductCard(
    product: ProductSummary,
    onOpen: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductThumbnail(imageUrl = product.imageUrl)
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Danh mục: ${product.category}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Giá khởi điểm: ${com.auction.app.util.FormatUtils.formatCurrency(product.startPrice, product.currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusChip(status = product.status)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Từ chối")
                }
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chấp nhận")
                }
            }
        }
    }
}
