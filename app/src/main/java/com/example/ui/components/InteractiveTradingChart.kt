package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CandleStick
import com.example.model.IndicatorType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InteractiveTradingChart(
    candles: List<CandleStick>,
    activeIndicators: Set<IndicatorType>,
    smaValues: List<Float>,
    emaValues: List<Float>,
    bollingerBands: Triple<List<Float>, List<Float>, List<Float>>,
    onCandleSelected: (CandleStick?) -> Unit,
    selectedCandle: CandleStick?,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier.background(Color(0xFF12141C)),
            contentAlignment = Alignment.Center
        ) {
            Text("No Chart Data Available", color = Color.Gray)
        }
        return
    }

    // Keep the last 40-50 candles for presentation scaling
    val visibleCandlesCount = 35
    val visibleCandles = if (candles.size > visibleCandlesCount) {
        candles.takeLast(visibleCandlesCount)
    } else {
        candles
    }

    val startIndex = candles.size - visibleCandles.size

    val maxPrice = remember(visibleCandles, activeIndicators) {
        var highest = visibleCandles.maxOf { it.high }
        if (activeIndicators.contains(IndicatorType.BOLLINGER)) {
            val bbUpper = bollingerBands.first
            if (bbUpper.size == candles.size) {
                val visibleUpper = bbUpper.drop(startIndex)
                highest = maxOf(highest, visibleUpper.maxOrNull() ?: highest)
            }
        }
        highest * 1.005f
    }

    val minPrice = remember(visibleCandles, activeIndicators) {
        var lowest = visibleCandles.minOf { it.low }
        if (activeIndicators.contains(IndicatorType.BOLLINGER)) {
            val bbLower = bollingerBands.third
            if (bbLower.size == candles.size) {
                val visibleLower = bbLower.drop(startIndex)
                lowest = minOf(lowest, visibleLower.filter { it > 0 }.minOrNull() ?: lowest)
            }
        }
        lowest * 0.995f
    }

    val priceRange = maxPrice - minPrice

    // Touch Interaction Variables
    var touchX by remember { mutableStateOf<Float?>(null) }
    var touchY by remember { mutableStateOf<Float?>(null) }

    val upColor = Color(0xFF00C853) // Neon Green
    val downColor = Color(0xFFFF1744) // Neon Crimson Red
    val gridColor = Color(0x1AFFFFFF)

    Column(
        modifier = modifier
            .background(Color(0xFF0C0E14), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(visibleCandles) {
                    detectTapGestures(
                        onTap = { offset ->
                            touchX = offset.x
                            touchY = offset.y
                        },
                        onPress = {
                            tryAwaitRelease()
                            touchX = null
                            touchY = null
                            onCandleSelected(null)
                        }
                    )
                }
                .pointerInput(visibleCandles) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            touchX = offset.x
                            touchY = offset.y
                        },
                        onDragEnd = {
                            touchX = null
                            touchY = null
                            onCandleSelected(null)
                        },
                        onDragCancel = {
                            touchX = null
                            touchY = null
                            onCandleSelected(null)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            touchX = (touchX ?: change.position.x) + dragAmount.x
                            touchY = (touchY ?: change.position.y) + dragAmount.y
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val candleWidth = width / visibleCandles.size
                val bodyPadding = candleWidth * 0.15f

                // Draw horizontal coordinate grids
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = (height / gridLines) * i
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )

                    // Draw price indicators along grid lines
                    val priceVal = maxPrice - (priceRange / gridLines) * i
                    val priceText = String.format(
                        if (priceVal > 1000) "$%,.1f" else if (priceVal > 5) "$%,.2f" else "$%,.4f",
                        priceVal
                    )
                }

                // --- Draw Bollinger Bands Shading ---
                if (activeIndicators.contains(IndicatorType.BOLLINGER)) {
                    val pathUpper = Path()
                    val pathLower = Path()
                    var firstPointSet = false

                    for (i in visibleCandles.indices) {
                        val originalIndex = startIndex + i
                        val uVal = bollingerBands.first.getOrNull(originalIndex) ?: 0f
                        val lVal = bollingerBands.third.getOrNull(originalIndex) ?: 0f

                        if (uVal > 0f && lVal > 0f) {
                            val x = (i * candleWidth) + (candleWidth / 2f)
                            val yUpper = height - ((uVal - minPrice) / priceRange) * height
                            val yLower = height - ((lVal - minPrice) / priceRange) * height

                            if (!firstPointSet) {
                                pathUpper.moveTo(x, yUpper)
                                pathLower.moveTo(x, yLower)
                                firstPointSet = true
                            } else {
                                pathUpper.lineTo(x, yUpper)
                                pathLower.lineTo(x, yLower)
                            }
                        }
                    }

                    if (firstPointSet) {
                        // Drawing shaded channel
                        val fillPath = Path().apply {
                            addPath(pathUpper)
                            for (i in visibleCandles.indices.reversed()) {
                                val originalIndex = startIndex + i
                                val lVal = bollingerBands.third.getOrNull(originalIndex) ?: 0f
                                if (lVal > 0f) {
                                    val x = (i * candleWidth) + (candleWidth / 2f)
                                    val yLower = height - ((lVal - minPrice) / priceRange) * height
                                    lineTo(x, yLower)
                                }
                            }
                            close()
                        }
                        drawPath(fillPath, color = Color(0x0F4CAF50))
                        drawPath(pathUpper, color = Color(0x334CAF50), style = Stroke(width = 3f))
                        drawPath(pathLower, color = Color(0x334CAF50), style = Stroke(width = 3f))
                    }
                }

                // --- Draw Candles ---
                for (i in visibleCandles.indices) {
                    val candle = visibleCandles[i]
                    val x = i * candleWidth

                    // Vertical mapping
                    val yHigh = height - ((candle.high - minPrice) / priceRange) * height
                    val yLow = height - ((candle.low - minPrice) / priceRange) * height
                    val yOpen = height - ((candle.open - minPrice) / priceRange) * height
                    val yClose = height - ((candle.close - minPrice) / priceRange) * height

                    val isBullish = candle.close >= candle.open
                    val themeColor = if (isBullish) upColor else downColor

                    // 1. Draw central wick line
                    drawLine(
                        color = themeColor,
                        start = Offset(x + (candleWidth / 2f), yHigh),
                        end = Offset(x + (candleWidth / 2f), yLow),
                        strokeWidth = 2f
                    )

                    // 2. Draw real body rectangle
                    val rectTop = minOf(yOpen, yClose)
                    val rectHeight = maxOf(Math.abs(yOpen - yClose), 2f)
                    drawRect(
                        color = themeColor,
                        topLeft = Offset(x + bodyPadding, rectTop),
                        size = Size(candleWidth - (bodyPadding * 2f), rectHeight),
                        style = if (isBullish) Stroke(width = 2.5f) else Fill
                    )

                    // 3. Draw Candlestick Pattern Identifiers
                    candle.detectedPattern?.let { pattern ->
                        val initialLetter = when {
                            pattern.contains("Hammer") -> "H"
                            pattern.contains("Doji") -> "D"
                            pattern.contains("Shooting") -> "S"
                            else -> "M"
                        }

                        val py = if (isBullish) yLow + 16f else yHigh - 16f
                        drawCircle(
                            color = if (initialLetter == "H") Color(0xFFFFEB3B) else Color(0xFF00E676),
                            radius = 12f,
                            center = Offset(x + (candleWidth / 2f), py),
                            style = Fill
                        )
                    }
                }

                // --- Superimpose SMAs & EMAs curves ---
                if (activeIndicators.contains(IndicatorType.SMA)) {
                    val pathSma = Path()
                    var firstPoint = false
                    for (i in visibleCandles.indices) {
                        val originalIndex = startIndex + i
                        val smaVal = smaValues.getOrNull(originalIndex) ?: 0f
                        if (smaVal > 0f) {
                            val x = (i * candleWidth) + (candleWidth / 2f)
                            val y = height - ((smaVal - minPrice) / priceRange) * height
                            if (!firstPoint) {
                                pathSma.moveTo(x, y)
                                firstPoint = true
                            } else {
                                pathSma.lineTo(x, y)
                            }
                        }
                    }
                    if (firstPoint) {
                        drawPath(pathSma, color = IndicatorType.SMA.color, style = Stroke(width = 4f))
                    }
                }

                if (activeIndicators.contains(IndicatorType.EMA)) {
                    val pathEma = Path()
                    var firstPoint = false
                    for (i in visibleCandles.indices) {
                        val originalIndex = startIndex + i
                        val emaVal = emaValues.getOrNull(originalIndex) ?: 0f
                        if (emaVal > 0f) {
                            val x = (i * candleWidth) + (candleWidth / 2f)
                            val y = height - ((emaVal - minPrice) / priceRange) * height
                            if (!firstPoint) {
                                pathEma.moveTo(x, y)
                                firstPoint = true
                            } else {
                                pathEma.lineTo(x, y)
                            }
                        }
                    }
                    if (firstPoint) {
                        drawPath(pathEma, color = IndicatorType.EMA.color, style = Stroke(width = 4f))
                    }
                }

                // --- Crosshair Interaction Pointer logic ---
                touchX?.let { tx ->
                    val hoverIndex = (tx / candleWidth).toInt().coerceIn(0, visibleCandles.size - 1)
                    val candle = visibleCandles[hoverIndex]
                    val cx = (hoverIndex * candleWidth) + (candleWidth / 2f)

                    // Draw vertical crosshair line
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(cx, 0f),
                        end = Offset(cx, height),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // Draw horizontal price pointer
                    touchY?.let { ty ->
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(0f, ty),
                            end = Offset(width, ty),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    // Report active hovered candle to VM
                    onCandleSelected(candle)
                }
            }
        }

        // --- Selection Tooltip Information panel ---
        selectedCandle?.let { candle ->
            val df = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val dateStr = df.format(Date(candle.timestamp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161A24)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Active Time: $dateStr", color = Color.Gray, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Open: ${String.format("%.2f", candle.open)}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "Close: ${String.format("%.2f", candle.close)}",
                                color = if (candle.close >= candle.open) upColor else downColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "High: ${String.format("%.2f", candle.high)}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "Low: ${String.format("%.2f", candle.low)}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    candle.detectedPattern?.let { pattern ->
                        Box(
                            modifier = Modifier
                                .background(Color(0x33FFEB3B), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                pattern,
                                color = Color(0xFFFFF176),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// RSI Oscillating Sub-chart
@Composable
fun RsiSubChart(
    rsiValues: List<Float>,
    modifier: Modifier = Modifier
) {
    if (rsiValues.isEmpty()) return
    val count = 35
    val visibleRsi = if (rsiValues.size > count) {
        rsiValues.takeLast(count)
    } else {
        rsiValues
    }

    Box(
        modifier = modifier
            .background(Color(0xFF0C0E14), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(
                "RSI Oscillating Dashboard",
                color = Color.LightGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Canvas(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) {
                val width = size.width
                val height = size.height
                val pointWidth = width / visibleRsi.size

                val gridColor = Color(0x13FFFFFF)

                // Standard RSI 30 / 70 horizontal oversold boundaries
                val y30 = height - (30f / 100f) * height
                val y70 = height - (70f / 100f) * height

                drawLine(color = Color(0x33E040FB), start = Offset(0f, y70), end = Offset(width, y70), strokeWidth = 2f)
                drawLine(color = Color(0x3300E5FF), start = Offset(0f, y30), end = Offset(width, y30), strokeWidth = 2f)

                // Rsi core path curve
                val rsiPath = Path()
                var first = false
                for (i in visibleRsi.indices) {
                    val rVal = visibleRsi[i].coerceIn(0f, 100f)
                    val x = (i * pointWidth) + (pointWidth / 2f)
                    val y = height - (rVal / 100f) * height

                    if (!first) {
                        rsiPath.moveTo(x, y)
                        first = true
                    } else {
                        rsiPath.lineTo(x, y)
                    }
                }

                if (first) {
                    drawPath(rsiPath, color = IndicatorType.RSI.color, style = Stroke(width = 4f))
                }
            }
        }
    }
}
