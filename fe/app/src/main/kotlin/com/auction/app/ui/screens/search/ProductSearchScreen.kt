package com.auction.app.ui.screens.search

import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.auction.app.LocalAppContainer
import com.auction.app.ui.components.AppScaffold
import com.auction.app.ui.components.NjAuctionItemCard
import com.auction.app.ui.navigation.Routes
import com.auction.app.ui.theme.*

@Composable
fun ProductSearchScreen(navController: NavController, onOpenProduct: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ProductSearchViewModel = viewModel(factory = ProductSearchViewModel.factory(container.repository))
    val uiState by viewModel.uiState.collectAsState()
    val isAdmin by container.tokenStore.isAdminFlow.collectAsState(initial = false)

    ProductSearchScreenContent(
        uiState = uiState,
        isAdmin = isAdmin,
        navController = navController,
        onKeywordChange = { viewModel.onKeywordChange(it) },
        onSearch = { viewModel.search() },
        onPageChange = { viewModel.search(it) },
        onOpenProduct = onOpenProduct
    )
}

@Composable
fun ProductSearchScreenContent(
    uiState: ProductSearchUiState,
    isAdmin: Boolean = false,
    navController: NavController?,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPageChange: (Int) -> Unit,
    onOpenProduct: (String) -> Unit
) {
    val context = LocalContext.current
    AppScaffold(
        navController = navController ?: androidx.navigation.compose.rememberNavController(),
        currentRoute = Routes.PRODUCT_SEARCH,
        title = "Danh sách đấu giá"
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = uiState.keyword,
                onValueChange = onKeywordChange,
                placeholder = { Text("Tìm kiếm sản phẩm...", fontSize = 13.sp, color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Navy, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Tìm", tint = Navy)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Navy,
                    unfocusedBorderColor = FieldBorder,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            PaginationBar(
                currentPage = uiState.currentPage,
                totalPages = uiState.totalPages,
                totalItems = uiState.products.size,
                totalElements = uiState.totalElements,
                onPageChange = onPageChange
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Navy)
                }
            } else if (uiState.error != null) {
                Text(text = uiState.error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp).weight(1f))
            } else if (uiState.products.isEmpty()) {
                Text(if (isAdmin) "Chưa có sản phẩm nào" else "Không có sản phẩm nào đang đấu giá", color = TextMuted, modifier = Modifier.padding(16.dp).weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(uiState.products) { index, product ->
                        NjAuctionItemCard(
                            product = product,
                            index = index,
                            onClick = { onOpenProduct(product.id) },
                            showStatus = isAdmin
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                PaginationBar(
                    currentPage = uiState.currentPage,
                    totalPages = uiState.totalPages,
                    totalItems = uiState.products.size,
                    totalElements = uiState.totalElements,
                    onPageChange = onPageChange
                )
            }
        }
    }
}

@Composable
fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    totalItems: Int,
    totalElements: Long,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    pageSize: Int = 10
) {
    val pages = totalPages.coerceAtLeast(1)
    val current = currentPage.coerceIn(1, pages)
    val rangeText = if (totalElements <= 0L || totalItems <= 0) {
        "0 sản phẩm"
    } else {
        val start = (current - 1) * pageSize + 1
        val end = start + totalItems - 1
        "$start-$end/$totalElements sản phẩm"
    }
    val windowSize = 5
    val first = (current - windowSize / 2).coerceIn(1, (pages - windowSize + 1).coerceAtLeast(1))
    val last = (first + windowSize - 1).coerceAtMost(pages)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rangeText,
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(end = 8.dp)
        )

        PageBox(text = "<", selected = false, enabled = current > 1) { onPageChange(current - 1) }
        for (page in first..last) {
            Spacer(modifier = Modifier.width(4.dp))
            PageBox(text = page.toString(), selected = page == current, enabled = page != current) { onPageChange(page) }
        }
        Spacer(modifier = Modifier.width(4.dp))
        PageBox(text = ">", selected = false, enabled = current < pages) { onPageChange(current + 1) }
    }
}

@Composable
private fun PageBox(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(if (selected) Navy else Color.Transparent)
            .border(0.8.dp, if (selected) Navy else Color(0xFFC7D6DF), RoundedCornerShape(2.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 11.sp,
            color = when {
                selected -> Color.White
                !enabled -> Color(0xFFC7D6DF)
                text == "<" || text == ">" -> TextMuted
                else -> TextDark
            }
        )
    }
}
