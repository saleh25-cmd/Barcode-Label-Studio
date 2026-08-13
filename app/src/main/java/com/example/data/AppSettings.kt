package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,
    val shopName: String = "محل النجاح",
    val logoUri: String? = null,
    val presetSize: String = "50x30", // 30x20, 40x25, 50x30, 60x40, 70x50, 100x50, CUSTOM
    val customWidthMm: Float = 50f,
    val customHeightMm: Float = 30f,
    val paperType: String = "A4", // A4, A5, LETTER, ROLL
    val columnsCount: Int = 3,
    val marginTopMm: Float = 10f,
    val marginBottomMm: Float = 10f,
    val marginLeftMm: Float = 10f,
    val marginRightMm: Float = 10f,
    val horizontalGapMm: Float = 3f,
    val verticalGapMm: Float = 3f,
    val lastUsedStartCode: Long = 1000L,
    val defaultPrice: String = "1200 DA"
) {
    fun getLabelDimensionsMm(): Pair<Float, Float> {
        return when (presetSize) {
            "30x20" -> Pair(30f, 20f)
            "40x25" -> Pair(40f, 25f)
            "50x30" -> Pair(50f, 30f)
            "60x40" -> Pair(60f, 40f)
            "70x50" -> Pair(70f, 50f)
            "100x50" -> Pair(100f, 50f)
            else -> Pair(customWidthMm.coerceAtLeast(15f), customHeightMm.coerceAtLeast(10f))
        }
    }
}
