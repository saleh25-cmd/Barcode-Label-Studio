package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSettings
import com.example.data.LabelItem
import com.example.util.Code128Encoder
import com.example.util.PdfExportManager

@Composable
fun LabelSheetPreview(
    labels: List<LabelItem>,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (labelWidthMm, labelHeightMm) = settings.getLabelDimensionsMm()

    val pageDim = PdfExportManager.getPageDimensions(settings.paperType)
    val pageAspect = pageDim.widthPt.toFloat() / pageDim.heightPt.toFloat()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(pageAspect)
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (labels.isEmpty()) {
            Text(
                text = "لا توجد ملصقات للمعاينة",
                color = Color.Gray,
                fontSize = 14.sp
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val scaleX = canvasWidth / pageDim.widthPt.toFloat()
                val scaleY = canvasHeight / pageDim.heightPt.toFloat()

                val mmToPt = 2.8346457f

                val marginLeft = settings.marginLeftMm * mmToPt * scaleX
                val marginRight = settings.marginRightMm * mmToPt * scaleX
                val marginTop = settings.marginTopMm * mmToPt * scaleY
                val marginBottom = settings.marginBottomMm * mmToPt * scaleY

                val labelWidth = labelWidthMm * mmToPt * scaleX
                val labelHeight = labelHeightMm * mmToPt * scaleY

                val gapX = settings.horizontalGapMm * mmToPt * scaleX
                val gapY = settings.verticalGapMm * mmToPt * scaleY

                val cols = settings.columnsCount.coerceAtLeast(1)
                val availableHeight = canvasHeight - marginTop - marginBottom
                val rows = Math.max(1, ((availableHeight + gapY) / (labelHeight + gapY)).toInt())

                // Draw printable margins area boundary line
                drawRect(
                    color = Color(0xFFE0E0E0),
                    topLeft = Offset(marginLeft, marginTop),
                    size = Size(canvasWidth - marginLeft - marginRight, canvasHeight - marginTop - marginBottom),
                    style = Stroke(width = 1f)
                )

                val flattened = mutableListOf<LabelItem>()
                for (item in labels) {
                    repeat(item.copies.coerceAtLeast(1)) {
                        flattened.add(item)
                    }
                }

                var index = 0
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        if (index >= flattened.size) break
                        val item = flattened[index]

                        val x = marginLeft + c * (labelWidth + gapX)
                        val y = marginTop + r * (labelHeight + gapY)

                        // Draw label outline card
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(x, y),
                            size = Size(labelWidth, labelHeight),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        drawRoundRect(
                            color = Color(0xFF6750A4),
                            topLeft = Offset(x, y),
                            size = Size(labelWidth, labelHeight),
                            cornerRadius = CornerRadius(4f, 4f),
                            style = Stroke(width = 1.5f)
                        )

                        // Draw scaled mini preview content
                        val bounds = android.graphics.RectF(
                            x + 2f,
                            y + (labelHeight * 0.25f),
                            x + labelWidth - 2f,
                            y + (labelHeight * 0.75f)
                        )

                        Code128Encoder.drawOnCanvas(
                            canvas = drawContext.canvas.nativeCanvas,
                            text = item.code,
                            bounds = bounds,
                            paintColor = android.graphics.Color.BLACK,
                            drawTextBelow = false
                        )

                        index++
                    }
                    if (index >= flattened.size) break
                }
            }
        }
    }
}
