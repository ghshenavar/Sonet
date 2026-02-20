package com.example.sonet.ui.components

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

@Composable
fun WorkoutBarChart(workoutCounts: Map<String, Int>) {
    if (workoutCounts.isEmpty()) return

    val maxCount = workoutCounts.values.max()
    val barColor = MaterialTheme.colorScheme.primary
    val typeface = Typeface.DEFAULT

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(start = 32.dp, end = 8.dp, bottom = 40.dp, top = 8.dp)
    ) {
        val entries = workoutCounts.entries.toList()
        val barCount = entries.size
        val totalWidth = size.width
        val totalHeight = size.height
        val barWidth = (totalWidth / barCount) * 0.6f
        val spacing = totalWidth / barCount

        entries.forEachIndexed { index, (name, count) ->
            val barHeight = (count.toFloat() / maxCount) * totalHeight
            val x = index * spacing + (spacing - barWidth) / 2
            val y = totalHeight - barHeight

            drawRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )

            val paint = android.graphics.Paint().apply {
                color = Color.DarkGray.toArgb()
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 28f
                this.typeface = typeface
            }

            drawContext.canvas.nativeCanvas.drawText(
                "$count",
                x + barWidth / 2,
                y - 6f,
                paint
            )

            drawContext.canvas.nativeCanvas.drawText(
                name.replaceFirstChar { it.uppercase() },
                x + barWidth / 2,
                totalHeight + 36f,
                paint
            )
        }

        drawLine(
            color = Color.Gray,
            start = Offset(0f, 0f),
            end = Offset(0f, totalHeight),
            strokeWidth = 2f
        )

        drawLine(
            color = Color.Gray,
            start = Offset(0f, totalHeight),
            end = Offset(totalWidth, totalHeight),
            strokeWidth = 2f
        )
    }
}