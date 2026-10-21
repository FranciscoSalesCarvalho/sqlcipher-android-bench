package dev.fsales.storage.db

import android.content.Context
import java.io.Closeable
import java.io.File

class BenchStore private constructor(
    private val db: BenchDatabase,
    val file: File,
) : Closeable {

    companion object {
        fun open(context: Context, arm: Arm, journal: Journal, passphrase: ByteArray): BenchStore =
            BenchStore(
                DatabaseFactory.open(context, arm, journal, passphrase),
                DatabaseFactory.dbFile(context, arm),
            )
    }

    /** Força a abertura do banco (e a derivação de chave no braço C) fora da medição. */
    fun warmUp() {
        db.openHelper.writableDatabase
    }

    fun insertBatch(rows: List<BenchRow>) = db.dao().insertAll(rows)

    fun insertOne(row: BenchRow): Long = db.dao().insert(row)

    fun count(): Int = db.dao().count()

    /** Leva o conteúdo do WAL para o arquivo principal. Em TRUNCATE, não faz nada. */
    fun checkpoint() {
        db.openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(TRUNCATE)")
            .use { it.moveToFirst() }
    }

    fun scalar(sql: String): String = try {
        db.openHelper.writableDatabase.query(sql).use { c ->
            if (c.moveToFirst()) c.getString(0) ?: "null" else "(vazio)"
        }
    } catch (e: Exception) {
        "erro: ${e.message}"
    }

    /** Tamanho em disco do banco e dos arquivos auxiliares. */
    fun sizeOnDisk(): Long =
        listOf("", "-wal", "-journal").sumOf { File(file.path + it).takeIf(File::exists)?.length() ?: 0L }

    fun column(sql: String): List<String> =
        db.openHelper.writableDatabase.query(sql).use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0) ?: "null") }
        }

    fun journalFile(): File = File(file.path + "-journal")

    override fun close() = db.close()
}