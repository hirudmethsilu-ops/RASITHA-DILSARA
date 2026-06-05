package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.BankTransaction
import java.text.SimpleDateFormat
import java.util.*

val CategoryColors = mapOf(
    "Food & Dining" to Color(0xFF6366F1), // Indigo
    "Shopping" to Color(0xFF8B5CF6),      // Violet
    "Groceries" to Color(0xFF10B981),     // Emerald
    "Transport" to Color(0xFFF59E0B),     // Amber
    "Bills & Utilities" to Color(0xFF3B82F6), // Blue
    "Health & Fitness" to Color(0xFF14B8A6),  // Teal
    "Entertainment" to Color(0xFFF43F5E),  // Rose
    "Income" to Color(0xFF10B981)          // Emerald
)

fun getCategoryColor(category: String): Color {
    return CategoryColors[category] ?: Color(0xFF64748B) // Slate
}

@Composable
fun DonutChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    thickness: Float = 40f
) {
    val total = data.values.sum()
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    if (total == 0.0) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No spending data",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        return
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val minSize = kotlin.math.min(width, height)
            val radius = (minSize - thickness) / 2f
            val center = Offset(width / 2f, height / 2f)

            var startAngle = -90f

            data.forEach { (category, value) ->
                val sweepAngle = ((value / total) * 360f).toFloat() * animatedProgress.value
                val color = getCategoryColor(category)

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = thickness, cap = StrokeCap.Round)
                )

                startAngle += sweepAngle
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Total Spent",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = String.format("$%.2f", total),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DailyTrendChart(
    transactions: List<BankTransaction>,
    modifier: Modifier = Modifier
) {
    // Computes transactions day-by-day in the current month
    val daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
    val dailySpent = DoubleArray(daysInMonth + 1)
    val dailyEarnt = DoubleArray(daysInMonth + 1)

    val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    transactions.forEach { tx ->
        val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
        if (cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear) {
            val day = cal.get(Calendar.DAY_OF_MONTH)
            if (day in 1..daysInMonth) {
                if (tx.amount < 0) {
                    dailySpent[day] += kotlin.math.abs(tx.amount)
                } else {
                    dailyEarnt[day] += tx.amount
                }
            }
        }
    }

    // Accumulate spent for running total graph
    val cumulativeSpent = DoubleArray(daysInMonth + 1)
    var cumulativeTotal = 0.0
    for (i in 1..daysInMonth) {
        cumulativeTotal += dailySpent[i]
        cumulativeSpent[i] = cumulativeTotal
    }

    val maxVal = kotlin.math.max(
        cumulativeSpent.maxOrNull() ?: 1.0,
        dailyEarnt.maxOrNull() ?: 1.0
    ).coerceAtLeast(100.0)

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(transactions) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200)
        )
    }

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val spendColor = Color(0xFFEF4444) // Rosy red
    val incomeColor = Color(0xFF10B981) // Emerald

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Draw horizontal grid lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = height - (i * (height / gridLines))
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (daysInMonth < 2) return@Canvas

            val stepX = width / (daysInMonth - 1)
            val spendPoints = mutableListOf<Offset>()
            val incomePoints = mutableListOf<Offset>()

            for (i in 1..daysInMonth) {
                val x = (i - 1) * stepX

                val spendY = height - ((cumulativeSpent[i] / maxVal) * height * animatedProgress.value).toFloat()
                spendPoints.add(Offset(x, spendY))

                // Income represents daily spikes
                val incomeY = height - ((dailyEarnt[i] / maxVal) * height * animatedProgress.value).toFloat()
                incomePoints.add(Offset(x, incomeY))
            }

            // Draw cumulative spending area path
            if (spendPoints.isNotEmpty()) {
                val spendPath = Path().apply {
                    moveTo(spendPoints[0].x, spendPoints[0].y)
                    for (i in 1 until spendPoints.size) {
                        // Smooth curves
                        val p0 = spendPoints[i - 1]
                        val p1 = spendPoints[i]
                        cubicTo(
                            (p0.x + p1.x) / 2, p0.y,
                            (p0.x + p1.x) / 2, p1.y,
                            p1.x, p1.y
                        )
                    }
                }

                // Fill gradient under spending curve
                val gradientPath = Path().apply {
                    addPath(spendPath)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }

                drawPath(
                    path = gradientPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            spendColor.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )

                drawPath(
                    path = spendPath,
                    color = spendColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Draw daily income points (represented as small circular spikes/bars)
            incomePoints.forEachIndexed { index, point ->
                if (dailyEarnt[index + 1] > 0) {
                    drawRect(
                        color = incomeColor.copy(alpha = 0.7f),
                        topLeft = Offset(point.x - 2.dp.toPx(), point.y),
                        size = Size(4.dp.toPx(), height - point.y)
                    )
                    drawCircle(
                        color = incomeColor,
                        radius = 3.dp.toPx(),
                        center = point
                    )
                }
            }
        }
    }
}
