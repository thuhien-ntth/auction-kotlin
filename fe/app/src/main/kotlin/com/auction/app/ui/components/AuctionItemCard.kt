package com.auction.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.auction.app.BuildConfig
import com.auction.app.network.model.ProductSummary
import com.auction.app.ui.theme.*

@Composable
fun NjAuctionItemCard(
    product: ProductSummary,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showStatus: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1B3B30))
            ) {
                if (product.imageUrl.isNullOrBlank()) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(38.dp)
                            .align(Alignment.Center)
                    )
                } else {
                    AsyncImage(
                        model = BuildConfig.API_BASE_URL + product.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))


            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                Text(
                    text = product.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Category, contentDescription = "Danh mục", tint = TextMuted, modifier = Modifier.size(11.dp))
                    Text(
                        " ${product.category}",
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(Icons.Default.Gavel, contentDescription = "Người tham gia", tint = TextMuted, modifier = Modifier.size(11.dp))
                    Text(" ${product.bidderCount}", fontSize = 10.sp, color = TextMuted)
                }

                Spacer(modifier = Modifier.height(3.dp))

                if (showStatus) {
                    StatusChip(status = product.status)
                    if (product.status == "REJECTED" && !product.rejectionReason.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Lý do: ${product.rejectionReason}",
                            fontSize = 10.sp,
                            color = Color(0xFFC62828),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(100.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Giá hiện tại ",
                        fontSize = 8.sp,
                        color = TextMuted
                    )
                    Text(
                        text = com.auction.app.util.FormatUtils.formatCurrency(product.startPrice, product.currency),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                }
            }
        }
    }
}
