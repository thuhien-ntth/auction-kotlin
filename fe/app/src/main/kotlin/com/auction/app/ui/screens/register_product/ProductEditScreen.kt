package com.auction.app.ui.screens.register_product

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.auction.app.BuildConfig
import com.auction.app.LocalAppContainer
import com.auction.app.ui.components.AppScaffold
import com.auction.app.ui.navigation.Routes
import com.auction.app.ui.theme.AuctionAppTheme

@Composable
fun ProductEditScreen(productId: String, navController: NavController) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val viewModel: ProductEditViewModel = viewModel(factory = ProductEditViewModel.factory(container.repository, productId))
    val uiState by viewModel.uiState.collectAsState()

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.onImageSelected(uri) }

    AppScaffold(navController = navController, currentRoute = "", title = "Cập nhật sản phẩm") { padding ->
        ProductEditScreenContent(
            uiState = uiState,
            padding = padding,
            onTitleChange = viewModel::onTitleChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onCategoryChange = viewModel::onCategoryChange,
            onCategoryDropdownExpandedChange = viewModel::onCategoryDropdownExpandedChange,
            onStartPriceChange = viewModel::onStartPriceChange,
            onCurrencyChange = viewModel::onCurrencyChange,
            onCurrencyDropdownExpandedChange = viewModel::onCurrencyDropdownExpandedChange,
            onAuctionStartAtChange = viewModel::onAuctionStartAtChange,
            onAuctionEndAtChange = viewModel::onAuctionEndAtChange,
            onPickImage = {
                pickImageLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            onSubmit = { viewModel.submitProduct(context) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditScreenContent(
    uiState: ProductEditUiState,
    padding: PaddingValues = PaddingValues(),
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onCategoryDropdownExpandedChange: (Boolean) -> Unit = {},
    onStartPriceChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit = {},
    onCurrencyDropdownExpandedChange: (Boolean) -> Unit = {},
    onAuctionStartAtChange: (String) -> Unit,
    onAuctionEndAtChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Thông tin sản phẩm đấu giá",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    label = { Text("Tên sản phẩm") },
                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Mô tả") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Dropdown danh mục — lấy từ API GET /categories
                ExposedDropdownMenuBox(
                    expanded = uiState.categoryDropdownExpanded,
                    onExpandedChange = onCategoryDropdownExpandedChange
                ) {
                    OutlinedTextField(
                        value = uiState.category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Danh mục") },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.categoryDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = uiState.categoryDropdownExpanded,
                        onDismissRequest = { onCategoryDropdownExpandedChange(false) }
                    ) {
                        if (uiState.categories.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Đang tải danh mục...") },
                                onClick = {}
                            )
                        } else {
                            uiState.categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = { onCategoryChange(cat.name) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Giá khởi điểm (trên) + dropdown đơn vị tiền tệ VND / USD / EUR (dưới)
                OutlinedTextField(
                    value = uiState.startPrice,
                    onValueChange = onStartPriceChange,
                    label = { Text("Giá khởi điểm") },
                    leadingIcon = { Icon(Icons.Default.Sell, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = uiState.currencyDropdownExpanded,
                    onExpandedChange = onCurrencyDropdownExpandedChange,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.currency,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("Đơn vị tiền tệ") },
                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.currencyDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = uiState.currencyDropdownExpanded,
                        onDismissRequest = { onCurrencyDropdownExpandedChange(false) }
                    ) {
                        com.auction.app.util.CurrencyOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.code) },
                                onClick = { onCurrencyChange(option.code) }
                            )
                        }
                    }
                }
                uiState.startPrice.toBigDecimalOrNull()?.let { p ->
                    Text(
                        text = com.auction.app.util.FormatUtils.formatCurrency(p, uiState.currency),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                DateTimePickerField(
                    label = "Thời gian bắt đầu",
                    isoValue = uiState.auctionStartAt,
                    onIsoChange = onAuctionStartAtChange
                )
                Spacer(modifier = Modifier.height(8.dp))
                DateTimePickerField(
                    label = "Thời gian kết thúc",
                    isoValue = uiState.auctionEndAt,
                    onIsoChange = onAuctionEndAtChange
                )
                Spacer(modifier = Modifier.height(4.dp))
                Spacer(modifier = Modifier.height(8.dp))

                // Chọn ảnh sản phẩm (tùy chọn) — upload sau khi đăng sản phẩm thành công.
                Text(
                    "Ảnh sản phẩm (tùy chọn)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        val newUri: Uri? = uiState.imageUri
                        when {
                            // Ưu tiên ảnh mới vừa chọn từ thiết bị
                            newUri != null -> AsyncImage(
                                model = newUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Nếu chưa chọn ảnh mới, hiển thị ảnh cũ từ server
                            uiState.existingImageUrl != null -> AsyncImage(
                                model = BuildConfig.API_BASE_URL + uiState.existingImageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Chưa có ảnh nào
                            else -> Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(onClick = onPickImage) {
                        Text(if (uiState.imageUri == null && uiState.existingImageUrl == null) "Chọn ảnh" else "Đổi ảnh khác")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.message != null) {
            Text(
                text = uiState.message ?: "",
                color = if (uiState.messageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        val price = uiState.startPrice.toBigDecimalOrNull()
        Button(
            enabled = !uiState.isSubmitting && uiState.title.isNotBlank() && uiState.category.isNotBlank() && price != null &&
                uiState.auctionStartAt.isNotBlank() && uiState.auctionEndAt.isNotBlank(),
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.isSubmitting) "Đang gửi..." else "Cập nhật sản phẩm")
        }
    }
}

// Chọn thời gian đấu giá bằng Date Picker + Time Picker của Material3 thay vì gõ tay chuỗi ISO-8601 —
// gõ tay dễ sai định dạng và trước đây người dùng phải tự quy đổi múi giờ VN sang UTC (xem
// KichBan_Demo_App.md). Composable này hiển thị giờ theo múi giờ của máy (dễ đọc), rồi tự quy đổi
// sang Instant/UTC đúng định dạng backend yêu cầu (auctionStartAt/auctionEndAt, xem
// ProductDtos.kt#CreateProductRequest) khi gọi onIsoChange.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerField(
    label: String,
    isoValue: String,
    onIsoChange: (String) -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    val displayFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm") }

    // Parse lại từ chuỗi ISO đã lưu ở uiState để mở lại vẫn thấy đúng giá trị cũ (vd. sau khi xoay màn hình).
    val currentInstant = remember(isoValue) {
        isoValue.trim().ifBlank { null }?.let { runCatching { Instant.parse(it) }.getOrNull() }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    // Ngày đã chọn ở bước 1 (Date Picker), giữ tạm để ghép với giờ chọn ở bước 2 (Time Picker).
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }

    val displayText = currentInstant?.let { displayFormatter.format(it.atZone(zone)) } ?: ""

    OutlinedCard(
        onClick = {
            pendingDate = (currentInstant ?: Instant.now()).atZone(zone).toLocalDate()
            showDatePicker = true
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    displayText.ifBlank { "Chọn ngày giờ" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (displayText.isBlank())
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    if (showDatePicker) {
        // DatePicker của Material3 làm việc với mốc UTC-midnight cho ngày được chọn — không dùng
        // trực tiếp millis này như epoch ở múi giờ máy, phải quy về LocalDate qua ZoneOffset.UTC.
        val initialMillis = (currentInstant ?: Instant.now()).atZone(zone).toLocalDate()
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        pendingDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        showDatePicker = false
                        showTimePicker = true
                    } else {
                        showDatePicker = false
                    }
                }) { Text("Tiếp theo") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Hủy") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val initialTime = currentInstant?.atZone(zone)?.toLocalTime() ?: LocalTime.of(8, 0)
        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = false // Mặt đồng hồ 1–12 + nút SA/CH; state.hour vẫn trả 0–23
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 6.dp) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Chọn giờ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    TimePicker(state = timePickerState)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Hủy") }
                        TextButton(onClick = {
                            val date = pendingDate
                            if (date != null) {
                                val localDateTime = LocalDateTime.of(
                                    date,
                                    LocalTime.of(timePickerState.hour, timePickerState.minute)
                                )
                                val instant = localDateTime.atZone(zone).toInstant()
                                onIsoChange(instant.toString())
                            }
                            showTimePicker = false
                        }) { Text("Xong") }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProductEditScreenPreview() {
    AuctionAppTheme {
        ProductEditScreenContent(
            uiState = ProductEditUiState(
                title = "Máy ảnh Leica M11",
                description = "Hàng chính hãng fullbox 99%",
                category = "ELECTRONICS",
                startPrice = "185000000"
            ),
            onTitleChange = {},
            onDescriptionChange = {},
            onCategoryChange = {},
            onStartPriceChange = {},
            onAuctionStartAtChange = {},
            onAuctionEndAtChange = {},
            onPickImage = {},
            onSubmit = {}
        )
    }
}
