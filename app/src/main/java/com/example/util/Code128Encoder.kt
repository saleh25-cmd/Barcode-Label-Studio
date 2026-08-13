package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/**
 * Standard Code 128 Encoder and Renderer.
 * Supports automatic Code 128B / 128C encoding with checksum calculation and vector/bitmap drawing.
 */
object Code128Encoder {

    private const val START_B = 104
    private const val START_C = 105
    private const val CODE_B = 100
    private const val CODE_C = 99
    private const val STOP = 106

    data class BarcodeData(
        val symbols: List<Int>,
        val modules: List<Boolean>, // true = bar, false = space
        val text: String
    )

    /**
     * Encodes input string into Code 128 module sequence.
     */
    fun encode(input: String): BarcodeData {
        val cleanInput = if (input.isEmpty()) "0" else input
        val symbols = mutableListOf<Int>()

        // Check if pure numbers with even length for Code 128C optimization
        val isPureDigits = cleanInput.all { it.isDigit() }
        
        if (isPureDigits && cleanInput.length >= 2 && cleanInput.length % 2 == 0) {
            // Use Code 128C
            symbols.add(START_C)
            var i = 0
            while (i < cleanInput.length) {
                val pair = cleanInput.substring(i, i + 2).toInt()
                symbols.add(pair)
                i += 2
            }
        } else {
            // Use Code 128B
            symbols.add(START_B)
            for (ch in cleanInput) {
                val codeVal = ch.code - 32
                if (codeVal in 0..95) {
                    symbols.add(codeVal)
                } else {
                    symbols.add('?'.code - 32)
                }
            }
        }

        // Calculate checksum: (Start + sum(pos * val)) % 103
        var checksum = symbols[0]
        for (idx in 1 until symbols.size) {
            checksum += idx * symbols[idx]
        }
        checksum %= 103
        symbols.add(checksum)

        // Append STOP symbol
        symbols.add(STOP)

        // Build boolean modules list
        val modules = mutableListOf<Boolean>()
        
        // 10 quiet zone modules
        repeat(10) { modules.add(false) }

        for (symIndex in symbols) {
            val patternStr = getPatternString(symIndex)
            var isBar = true
            for (charDigit in patternStr) {
                val count = charDigit.toString().toInt()
                repeat(count) {
                    modules.add(isBar)
                }
                isBar = !isBar
            }
        }

        // Add 10 quiet zone modules
        repeat(10) { modules.add(false) }

        return BarcodeData(symbols, modules, cleanInput)
    }

    private fun getPatternString(index: Int): String {
        return when (index) {
            0 -> "212222"; 1 -> "222122"; 2 -> "222221"; 3 -> "121223"; 4 -> "121322"; 5 -> "131222"; 6 -> "122213"; 7 -> "122312"; 8 -> "132212"; 9 -> "221213"
            10 -> "221312"; 11 -> "231212"; 12 -> "112232"; 13 -> "122132"; 14 -> "122231"; 15 -> "113222"; 16 -> "123122"; 17 -> "123221"; 18 -> "223211"; 19 -> "221132"
            20 -> "221231"; 21 -> "213212"; 22 -> "223112"; 23 -> "312131"; 24 -> "311222"; 25 -> "321122"; 26 -> "321221"; 27 -> "312212"; 28 -> "322112"; 29 -> "322211"
            30 -> "212123"; 31 -> "212321"; 32 -> "232121"; 33 -> "111323"; 34 -> "131123"; 35 -> "131321"; 36 -> "112313"; 37 -> "132113"; 38 -> "132311"; 39 -> "211313"
            40 -> "231113"; 41 -> "231311"; 42 -> "112133"; 43 -> "112331"; 44 -> "132131"; 45 -> "113123"; 46 -> "113321"; 47 -> "133121"; 48 -> "313121"; 49 -> "211331"
            50 -> "231131"; 51 -> "213113"; 52 -> "213311"; 53 -> "213131"; 54 -> "311123"; 55 -> "311321"; 56 -> "331121"; 57 -> "312113"; 58 -> "312311"; 59 -> "332111"
            60 -> "314111"; 61 -> "221411"; 62 -> "431111"; 63 -> "111224"; 64 -> "111422"; 65 -> "121124"; 66 -> "121421"; 67 -> "141122"; 68 -> "141221"; 69 -> "112214"
            70 -> "112412"; 71 -> "122114"; 72 -> "122411"; 73 -> "142112"; 74 -> "142211"; 75 -> "241211"; 76 -> "221114"; 77 -> "413111"; 78 -> "241112"; 79 -> "134111"
            80 -> "111242"; 81 -> "121142"; 82 -> "121241"; 83 -> "114212"; 84 -> "124112"; 85 -> "124211"; 86 -> "411212"; 87 -> "421112"; 88 -> "421211"; 89 -> "212141"
            90 -> "214121"; 91 -> "412121"; 92 -> "111143"; 93 -> "111341"; 94 -> "131141"; 95 -> "114113"; 96 -> "114311"; 97 -> "411113"; 98 -> "411311"; 99 -> "113141"
            100 -> "114131"; 101 -> "311141"; 102 -> "411131"; 103 -> "211412"; 104 -> "211214"; 105 -> "211232"; 106 -> "2331112"
            else -> "211214"
        }
    }

    /**
     * Renders barcode onto an existing Canvas within specified bounding box.
     * Uses contiguous module grouping to ensure zero-bleed crisp barcode bars.
     */
    fun drawOnCanvas(
        canvas: Canvas,
        text: String,
        bounds: RectF,
        paintColor: Int = Color.BLACK,
        drawTextBelow: Boolean = false
    ) {
        val barcodeData = encode(text)
        val modules = barcodeData.modules
        val totalModules = modules.size
        if (totalModules == 0) return

        val moduleWidth = bounds.width() / totalModules
        if (moduleWidth <= 0f) return

        val textHeight = if (drawTextBelow) (bounds.height() * 0.22f).coerceIn(8f, 22f) else 0f
        val barHeight = (bounds.height() - textHeight - (if (drawTextBelow) 2f else 0f)).coerceAtLeast(1f)

        val paint = Paint().apply {
            color = paintColor
            style = Paint.Style.FILL
            isAntiAlias = false
        }

        // Group contiguous black modules into single precise bar rectangles to avoid gaps & overlap
        var barStartModule = -1
        var barWidthModules = 0

        for (i in modules.indices) {
            if (modules[i]) {
                if (barStartModule == -1) {
                    barStartModule = i
                    barWidthModules = 1
                } else {
                    barWidthModules++
                }
            } else {
                if (barStartModule != -1) {
                    val left = bounds.left + (barStartModule * moduleWidth)
                    val right = bounds.left + ((barStartModule + barWidthModules) * moduleWidth)
                    canvas.drawRect(left, bounds.top, right, bounds.top + barHeight, paint)
                    barStartModule = -1
                    barWidthModules = 0
                }
            }
        }
        if (barStartModule != -1) {
            val left = bounds.left + (barStartModule * moduleWidth)
            val right = bounds.left + ((barStartModule + barWidthModules) * moduleWidth)
            canvas.drawRect(left, bounds.top, right, bounds.top + barHeight, paint)
        }

        if (drawTextBelow && text.isNotBlank()) {
            val textPaint = Paint().apply {
                color = paintColor
                textSize = textHeight
                isAntiAlias = true
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                text,
                bounds.centerX(),
                bounds.bottom - 1f,
                textPaint
            )
        }
    }

    /**
     * Generates a clean Bitmap image of the barcode.
     */
    fun generateBitmap(text: String, width: Int = 400, height: Int = 120): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val bounds = RectF(10f, 10f, width - 10f, height - 10f)
        drawOnCanvas(canvas, text, bounds, Color.BLACK, false)
        return bitmap
    }
}
