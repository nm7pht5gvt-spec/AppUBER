package com.tuapp.tripadvisor.ui.overlay

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuapp.tripadvisor.domain.model.SemaphoreStatus
import com.tuapp.tripadvisor.domain.model.TripEvaluation

private val RedColor = Color(0xFFE53935)
private val YellowColor = Color(0xFFFDD835)
private val GreenColor = Color(0xFF43A047)
private val DarkBackground = Color(0xFF1B1B1B)

@Composable
fun SemaphoreWidget(
    evaluation: TripEvaluation,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (evaluation.status) {
            SemaphoreStatus.RED -> RedColor
            SemaphoreStatus.YELLOW -> YellowColor
            SemaphoreStatus.GREEN -> GreenColor
        },
        animationSpec = tween(durationMillis = 300),
        label = "semaphoreColor"
    )

    Column(
        modifier = modifier
            .width(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkBackground)
            .border(2.dp, statusColor, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusLabel(evaluation.status),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        BreakdownRow(
            label = "Por km",
            value = "$${"%.2f".format(evaluation.realPricePerKm)}",
            diffPercent = evaluation.pricePerKmDiffPercent
        )

        Spacer(modifier = Modifier.height(4.dp))

        BreakdownRow(
            label = "Por hora",
            value = "$${"%.2f".format(evaluation.realEarningsPerHour)}",
            diffPercent = evaluation.earningsPerHourDiffPercent
        )
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    diffPercent: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.LightGray, fontSize = 13.sp)
        Column(horizontalAlignment = Alignment.End) {
            Text(text = value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                text = "${if (diffPercent >= 0) "+" else ""}${"%.0f".format(diffPercent)}%",
                color = if (diffPercent >= 0) GreenColor else RedColor,
                fontSize = 11.sp
            )
        }
    }
}

private fun statusLabel(status: SemaphoreStatus): String = when (status) {
    SemaphoreStatus.RED -> "NO CONVIENE"
    SemaphoreStatus.YELLOW -> "JUSTO"
    SemaphoreStatus.GREEN -> "CONVIENE"
}
