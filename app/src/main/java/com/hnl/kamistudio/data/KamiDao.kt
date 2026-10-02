package com.hnl.kamistudio.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KamiDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(kami: KamiEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(kamiList: List<KamiEntity>)

    @Update
    suspend fun update(kami: KamiEntity)

    @Delete
    suspend fun delete(kami: KamiEntity)

    @Query("DELETE FROM kami_codes WHERE code = :code")
    suspend fun deleteByCode(code: String)

    @Query("DELETE FROM kami_codes")
    suspend fun deleteAll()

    @Query("SELECT * FROM kami_codes ORDER BY createdAt DESC")
    fun getAll(): Flow<List<KamiEntity>>

    @Query("SELECT * FROM kami_codes WHERE status = :status ORDER BY createdAt DESC")
    fun getByStatus(status: String): Flow<List<KamiEntity>>

    @Query("SELECT * FROM kami_codes WHERE code LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun search(query: String): Flow<List<KamiEntity>>

    @Query("SELECT COUNT(*) FROM kami_codes")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM kami_codes WHERE status = :status")
    suspend fun getCountByStatus(status: String): Int

    @Query("SELECT * FROM kami_codes WHERE code = :code LIMIT 1")
    suspend fun findByCode(code: String): KamiEntity?
}
