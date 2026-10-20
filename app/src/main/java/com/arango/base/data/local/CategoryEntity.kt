package com.arango.base.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconKey: String = "category",
    val colorHex: Long = 0xFF5B604BL,
    val spendingTarget: Double? = null,
    val displayOrder: Int = 0
)
