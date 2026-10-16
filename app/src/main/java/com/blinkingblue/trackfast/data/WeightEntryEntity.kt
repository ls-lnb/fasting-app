package com.blinkingblue.trackfast.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.blinkingblue.trackfast.domain.WeightEntry

@Entity(
    tableName = "weight_entries",
    indices = [Index("recordedEpochMillis")],
)
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weightKg: Double,
    val recordedEpochMillis: Long,
    val createdEpochMillis: Long,
) {
    fun toDomain(): WeightEntry = WeightEntry(
        id = id,
        weightKg = weightKg,
        recordedEpochMillis = recordedEpochMillis,
        createdEpochMillis = createdEpochMillis,
    )
}

fun WeightEntry.toEntity(): WeightEntryEntity = WeightEntryEntity(
    id = id,
    weightKg = weightKg,
    recordedEpochMillis = recordedEpochMillis,
    createdEpochMillis = createdEpochMillis,
)
