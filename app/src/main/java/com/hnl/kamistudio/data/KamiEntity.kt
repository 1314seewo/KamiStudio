package com.hnl.kamistudio.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kami_codes")
data class KamiEntity(
    @PrimaryKey val code: String,
    val type: String,
    val prefix: String,
    val validDays: Int,
    val status: String = "inactive", // inactive, active, expired, disabled
    val createdAt: Long = System.currentTimeMillis(),
    val activatedAt: Long? = null,
    val deviceId: String? = null,
    val batchName: String = "",
    val note: String = ""
)
