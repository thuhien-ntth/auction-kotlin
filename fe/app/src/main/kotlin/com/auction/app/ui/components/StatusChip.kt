package com.auction.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.auction.app.ui.theme.*

@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val style = statusStyle(status)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = style.bg,
        modifier = modifier
    ) {
        Text(
            text = style.label,
            color = style.fg,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

private data class StatusStyle(val label: String, val bg: Color, val fg: Color)

private fun statusStyle(status: String): StatusStyle = when (status) {
    "PENDING_APPROVAL" -> StatusStyle("Đang chờ", StatusPendingBg, StatusPendingFg)
    "APPROVED" -> StatusStyle("Đã duyệt", StatusApprovedBg, StatusApprovedFg)
    "ACTIVE" -> StatusStyle("Đang đấu giá", StatusActiveBg, StatusActiveFg)
    "REJECTED" -> StatusStyle("Bị từ chối", StatusRejectedBg, StatusRejectedFg)
    "SOLD" -> StatusStyle("Đã bán", StatusSoldBg, StatusSoldFg)
    "ENDED_NO_BID" -> StatusStyle("Kết thúc, không có giá", StatusPendingBg, StatusPendingFg)
    "SCHEDULED" -> StatusStyle("Đã lên lịch", StatusPendingBg, StatusPendingFg)
    else -> StatusStyle(status, StatusPendingBg, StatusPendingFg)
}
