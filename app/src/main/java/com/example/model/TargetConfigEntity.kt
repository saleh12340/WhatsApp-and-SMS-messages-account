package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "target_configs")
data class TargetConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val targetType: String, // "SMS" أو "WHATSAPP"
    val identifier: String,  // مثلا: "AmalbwadiEX" أو "حوالات"
    val isEnabled: Boolean = true
)
