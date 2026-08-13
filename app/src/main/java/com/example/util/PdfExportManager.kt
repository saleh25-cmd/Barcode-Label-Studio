package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.data.AppSettings
import com.example.data.LabelItem
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object PdfExportManager {

    private const val MM_TO_PT = 2.8346457f // 1 mm = 2.8346457 points (72 dpi)

    data class PageDimensions(
        val widthPt: Int,
        val heightPt: Int,
        val name: String
    )

    fun getPageDimensions(paperType: String): PageDimensions {
        return when (paperType.uppercase()) {
            "A5" -> PageDimensions((148 * MM_TO_PT).toInt(), (210 * MM_TO_PT).toInt(), "A5")
            "LETTER" -> PageDimensions((215.9f * MM_TO_PT).toInt(), (279.4f * MM_TO_PT).toInt(), "Letter")
            else -> PageDimensions((210 * MM_TO_PT).toInt(), (297 * MM_TO_PT).toInt(), "A4") // A4 default
        }
    }

    /**
     * Generates a PDF file containing all labels structured according to page settings.
     */
    fun generatePdfFile(
        context: Context,
        labels: List<LabelItem>,
        settings: AppSettings,
        outputFile: File
    ): Boolean {
        val pdfDoc = PdfDocument()
        val pageDim = getPageDimensions(settings.paperType)

        val (labelWidthMm, labelHeightMm) = settings.getLabelDimensionsMm()
        val labelWidthPt = labelWidthMm * MM_TO_PT
        val labelHeightPt = labelHeightMm * MM_TO_PT

        val marginTopPt = settings.marginTopMm * MM_TO_PT
        val marginBottomPt = settings.marginBottomMm * MM_TO_PT
        val marginLeftPt = settings.marginLeftMm * MM_TO_PT
        val marginRightPt = settings.marginRightMm * MM_TO_PT
        val gapXPt = settings.horizontalGapMm * MM_TO_PT
        val gapYPt = settings.verticalGapMm * MM_TO_PT

        val availableWidthPt = pageDim.widthPt - marginLeftPt - marginRightPt
        val availableHeightPt = pageDim.heightPt - marginTopPt - marginBottomPt

        val cols = settings.columnsCount.coerceAtLeast(1)
        val rows = Math.max(1, ((availableHeightPt + gapYPt) / (labelHeightPt + gapYPt)).toInt())

        val labelsPerPage = cols * rows

        // Expand labels according to copies count
        val flattenedLabels = mutableListOf<LabelItem>()
        for (item in labels) {
            repeat(item.copies.coerceAtLeast(1)) {
                flattenedLabels.add(item)
            }
        }

        if (flattenedLabels.isEmpty()) {
            pdfDoc.close()
            return false
        }

        val totalPages = Math.ceil(flattenedLabels.size.toDouble() / labelsPerPage).toInt()

        val logoCache = mutableMapOf<String, Bitmap?>()

        var labelIndex = 0
        for (pageNum in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageDim.widthPt, pageDim.heightPt, pageNum).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            // Fill page white
            canvas.drawColor(Color.WHITE)

            // Draw labels grid
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    if (labelIndex >= flattenedLabels.size) break

                    val item = flattenedLabels[labelIndex]

                    val x = marginLeftPt + col * (labelWidthPt + gapXPt)
                    val y = marginTopPt + row * (labelHeightPt + gapYPt)

                    val labelRect = RectF(x, y, x + labelWidthPt, y + labelHeightPt)

                    drawSingleLabelOnCanvas(context, canvas, item, settings, labelRect, logoCache)

                    labelIndex++
                }
                if (labelIndex >= flattenedLabels.size) break
            }

            pdfDoc.finishPage(page)
        }

        return try {
            val os = FileOutputStream(outputFile)
            pdfDoc.writeTo(os)
            os.close()
            pdfDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDoc.close()
            false
        }
    }

    /**
     * Draws one label card strictly inside specified bounds on a PDF/Preview Canvas.
     */
    fun drawSingleLabelOnCanvas(
        context: Context,
        canvas: Canvas,
        item: LabelItem,
        settings: AppSettings,
        bounds: RectF,
        logoCache: MutableMap<String, Bitmap?> = mutableMapOf()
    ) {
        val padding = 4f

        // Border rectangle
        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        canvas.drawRect(bounds, borderPaint)

        val innerRect = RectF(
            bounds.left + padding,
            bounds.top + padding,
            bounds.right - padding,
            bounds.bottom - padding
        )

        val shopName = if (item.shopName.isNotBlank()) item.shopName else settings.shopName
        val logoUri = item.logoUri ?: settings.logoUri
        val price = if (item.price.isNotBlank()) item.price else settings.defaultPrice

        var currentTop = innerRect.top

        // 1. TOP HEADER: Logo & Shop Name
        val hasHeader = shopName.isNotBlank() || (logoUri != null && logoUri.isNotBlank())
        val headerHeight = if (hasHeader) innerRect.height() * 0.22f else 0f

        if (hasHeader) {
            val headerRect = RectF(innerRect.left, currentTop, innerRect.right, currentTop + headerHeight)

            var logoWidth = 0f
            if (logoUri != null && logoUri.isNotBlank()) {
                val logoBitmap = logoCache.getOrPut(logoUri) {
                    loadBitmapFromUri(context, logoUri)
                }
                if (logoBitmap != null) {
                    val aspect = logoBitmap.width.toFloat() / logoBitmap.height.toFloat()
                    logoWidth = headerHeight * aspect
                    val logoRect = RectF(headerRect.left, headerRect.top, headerRect.left + logoWidth, headerRect.bottom)
                    canvas.drawBitmap(logoBitmap, null, logoRect, Paint(Paint.FILTER_BITMAP_FLAG))
                }
            }

            if (shopName.isNotBlank()) {
                val shopTextPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = headerHeight * 0.65f
                    isAntiAlias = true
                    isFakeBoldText = true
                    textAlign = if (logoWidth > 0) Paint.Align.LEFT else Paint.Align.CENTER
                }
                val textX = if (logoWidth > 0) headerRect.left + logoWidth + 4f else headerRect.centerX()
                canvas.drawText(shopName, textX, headerRect.centerY() + (shopTextPaint.textSize / 3), shopTextPaint)
            }

            currentTop += headerHeight + 2f
        }

        // 2. MIDDLE: CODE 128 BARCODE & DIGITS
        val priceHeight = if (price.isNotBlank()) innerRect.height() * 0.22f else 0f
        val barcodeAreaHeight = (innerRect.bottom - currentTop - priceHeight - 2f).coerceAtLeast(10f)

        val barcodeBounds = RectF(innerRect.left + 2f, currentTop, innerRect.right - 2f, currentTop + barcodeAreaHeight)
        Code128Encoder.drawOnCanvas(canvas, item.code, barcodeBounds, Color.BLACK, true)

        currentTop += barcodeAreaHeight + 2f

        // 3. BOTTOM: PRICE TAG
        if (price.isNotBlank()) {
            val priceRect = RectF(innerRect.left, innerRect.bottom - priceHeight, innerRect.right, innerRect.bottom)

            val priceBgPaint = Paint().apply {
                color = Color.parseColor("#F3EDF7")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(priceRect, 4f, 4f, priceBgPaint)

            val priceTextPaint = Paint().apply {
                color = Color.parseColor("#6750A4")
                textSize = priceRect.height() * 0.65f
                isAntiAlias = true
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                price,
                priceRect.centerX(),
                priceRect.centerY() + (priceTextPaint.textSize / 3),
                priceTextPaint
            )
        }
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }

    private fun loadBitmapFromUri(context: Context, uriStr: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriStr)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Direct print using Android PrintManager.
     */
    fun printDocument(context: Context, pdfFile: File) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val pdi = PrintDocumentInfo.Builder("Barcode_Labels.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(pdi, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    val inStream = pdfFile.inputStream()
                    val outStream = FileOutputStream(destination?.fileDescriptor)
                    inStream.copyTo(outStream)
                    inStream.close()
                    outStream.close()
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }
        printManager.print("Barcode Label Studio Job", printAdapter, null)
    }
}
