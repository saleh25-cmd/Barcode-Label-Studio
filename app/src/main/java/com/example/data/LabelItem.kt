package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "label_items")
data class LabelItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val shopName: String = "",
    val price: String = "",
    val logoUri: String? = null,
    val copies: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)
