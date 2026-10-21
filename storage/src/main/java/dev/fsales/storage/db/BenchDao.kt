package dev.fsales.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BenchDao {

    // S1: uma linha por chamada → uma transação por linha (autocommit)
    @Insert
    fun insert(row: BenchRow): Long

    // S2: todas as linhas de uma vez
    @Insert
    fun insertAll(rows: List<BenchRow>)

    // S3: reescreve o payload de uma linha
    @Query("UPDATE bench_row SET payload = :payload WHERE id = :id")
    fun updatePayload(id: Long, payload: ByteArray): Int

    // S4: apaga um conjunto de linhas
    @Query("DELETE FROM bench_row WHERE id IN (:ids)")
    fun deleteByIds(ids: List<Long>): Int

    // Apoio: sortear linhas para S3 e S4
    @Query("SELECT id FROM bench_row")
    fun allIds(): List<Long>

    // Apoio: conferir se o preparo inseriu o que devia
    @Query("SELECT COUNT(*) FROM bench_row")
    fun count(): Int
}
