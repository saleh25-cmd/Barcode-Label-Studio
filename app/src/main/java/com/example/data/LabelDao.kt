package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {
    @Query("SELECT * FROM label_items ORDER BY createdAt DESC")
    fun getAllLabels(): Flow<List<LabelItem>>

    @Query("SELECT * FROM label_items WHERE code LIKE '%' || :query || '%' OR price LIKE '%' || :query || '%' OR shopName LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchLabels(query: String): Flow<List<LabelItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: LabelItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabels(labels: List<LabelItem>)

    @Update
    suspend fun updateLabel(label: LabelItem)

    @Delete
    suspend fun deleteLabel(label: LabelItem)

    @Query("DELETE FROM label_items")
    suspend fun deleteAllLabels()

    @Query("SELECT COUNT(*) FROM label_items")
    suspend fun getLabelCount(): Int
}
