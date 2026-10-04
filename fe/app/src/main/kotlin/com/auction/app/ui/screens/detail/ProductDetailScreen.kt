package com.auction.app.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.auction.app.LocalAppContainer
import com.auction.app.ui.components.ProductThumbnail
import com.auction.app.ui.components.StatusChip
import com.auction.app.ui.navigation.Routes
import com.auction.app.ui.theme.AuctionGold90
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(productId: String, navController: NavController) {
    val container = LocalAppContainer.current
    val viewModel: ProductDetailViewModel = viewModel(factory = ProductDetailViewModel.factory(container.repository, productId))
    val uiState by viewModel.uiState.collectAsState()

    ProductDetailScreenContent(
        uiState = uiState,
        targetBidPrice = viewModel.targetBidPrice,
        navController = navController,
        onNavigateBack = { navController.navigateUp() },
        onSelectStep = viewModel::selectStep,
        onShowConfirmDialog = viewModel::showConfirmDialog,
        onDismissConfirmDialog = viewModel::dismissConfirmDialog,
        onPlaceBid = viewModel::placeBid,
        onApprove = viewModel::approveProduct,
        onShowRejectDialog = viewModel::showRejectDialog,
        onDismissRejectDialog = viewModel::dismissRejectDialog,
        onReject = viewModel::rejectProduct
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreenContent(
    uiState: ProductDetailUiState,
    targetBidPrice: BigDecimal,
    navController: NavController,
    onNavigateBack: () -> Unit,
    onSelectStep: (BigDecimal) -> Unit,
    onShowConfirmDialog: () -> Unit,
    onDismissConfirmDialog: () -> Unit,
    onPlaceBid: () -> Unit,
    onApprove: () -> Unit = {},
    onShowRejectDialog: () -> Unit = {},
    onDismissRejectDialog: () -> Unit = {},
    onReject: (String) -> Unit = {}
) {
    val detail = uiState.detail
    val currency = uiState.detail?.currency ?: "VND"

    com.auction.app.ui.components.AppScaffold(
        navController = navController,
        currentRoute = "",
        title = "Chi tiết sản phẩm"
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                if (detail == null) {
                    Text(uiState.message ?: "Không tìm thấy sản phẩm.", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProductThumbnail(size = 80.dp, imageUrl = detail.imageUrl)
                                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                    Text(
                                        text = detail.title,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = detail.category,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.padding(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StatusChip(status = detail.status)
                                    }
                                }
                            }
                        }

                        if (uiState.isCurrentHighestBidder && detail.status == "ACTIVE") {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(AuctionGold90, RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    Text(
                                        text = "Bạn đang giữ giá cao nhất cho sản phẩm này",
                                        modifier = Modifier.padding(start = 8.dp),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        item {
                            Text(text = detail.description, style = MaterialTheme.typography.bodyLarge)
                        }

                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Giá hiện tại: ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = com.auction.app.util.FormatUtils.formatCurrency(detail.currentPrice, currency),
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Giá khởi điểm: ${com.auction.app.util.FormatUtils.formatCurrency(detail.startPrice, currency)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    if (detail.status == "REJECTED" && detail.rejectionReason != null) {
                                        Spacer(modifier = Modifier.padding(4.dp))
                                        Text(
                                            text = "Lý do từ chối: ${detail.rejectionReason}",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.message != null) {
                            item {
                                Text(
                                    text = uiState.message ?: "",
                                    color = if (uiState.messageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        item {
                            HorizontalDivider()
                            Text(
                                text = "Lịch sử đặt giá",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        if (uiState.history.isEmpty()) {
                            item { Text("Chưa có lượt đặt giá nào.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        } else {
                            items(uiState.history) { b ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = com.auction.app.util.FormatUtils.formatCurrency(b.amount, currency), fontWeight = FontWeight.Medium)
                                    Text(
                                        text = if (b.accepted) "Chấp nhận" else "Từ chối",
                                        color = if (b.accepted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
                }
            }

            if (detail != null && detail.status == "PENDING_APPROVAL" && uiState.myUserId == detail.sellerId && !uiState.isAdmin) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = { navController.navigate(Routes.productEdit(detail.id)) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Chỉnh sửa thông tin") }
                    }
                }
            }

            if (detail != null && uiState.isAdmin && detail.status == "PENDING_APPROVAL") {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onShowRejectDialog,
                            enabled = !uiState.isReviewing,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) { Text("Từ chối") }
                        Button(
                            onClick = onApprove,
                            enabled = !uiState.isReviewing,
                            modifier = Modifier.weight(1f)
                        ) { Text(if (uiState.isReviewing) "Đang xử lý..." else "Chấp nhận") }
                    }
                }
            }

            val isMyProduct = detail != null && uiState.myUserId != null && uiState.myUserId == detail.sellerId
            if (detail != null && detail.status == "ACTIVE" && !uiState.isAdmin && isMyProduct) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Đây là sản phẩm của bạn, bạn không thể tham gia đấu giá.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                }
            }

            if (detail != null && detail.status == "ACTIVE" && !uiState.isAdmin && !isMyProduct) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (uiState.isCurrentHighestBidder) Color(0xFFFFD700) else Color.LightGray.copy(alpha = 0.5f),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "Quyền trúng đấu giá",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isCurrentHighestBidder) Color.Black else Color.DarkGray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (uiState.isCurrentHighestBidder) {
                                Text(
                                    text = "Bạn đang trả giá cao nhất",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Text(
                            text = "Chọn bước nhảy:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.availableSteps) { step ->
                                val isSelected = (step == uiState.selectedStep)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectStep(step) },
                                    label = {
                                        Text(
                                            text = "+${com.auction.app.util.FormatUtils.formatPrice(step)}",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = onShowConfirmDialog,
                            enabled = !uiState.isPlacing && !uiState.isCurrentHighestBidder,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (uiState.isCurrentHighestBidder) "Bạn đang giữ giá cao nhất"
                                else "Đặt giá: ${com.auction.app.util.FormatUtils.formatCurrency(targetBidPrice, currency)}"
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showRejectDialog) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = onDismissRejectDialog,
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
                    onClick = { onReject(reason.trim()) }
                ) { Text("Từ chối", color = if (reason.isNotBlank()) MaterialTheme.colorScheme.error else Color.Unspecified) }
            },
            dismissButton = {
                TextButton(onClick = onDismissRejectDialog) { Text("Hủy") }
            }
        )
    }

    if (uiState.showConfirmDialog && detail != null) {
        AlertDialog(
            onDismissRequest = onDismissConfirmDialog,
            title = {
                Text(
                    text = "Xác nhận đặt giá",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Sản phẩm đấu giá: ${detail.title} (${detail.category})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Giá hiện tại: ${com.auction.app.util.FormatUtils.formatCurrency(detail.currentPrice, currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Giá đặt thầu: ${com.auction.app.util.FormatUtils.formatCurrency(targetBidPrice, currency)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Bạn có chắc chắn muốn đặt giá với mức giá trên không?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(onClick = onPlaceBid) {
                    Text("Xác nhận đặt giá")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissConfirmDialog) {
                    Text("Hủy")
                }
            }
        )
    }
}
