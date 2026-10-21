package dev.fsales.storage.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabase.JournalMode
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

enum class Arm {
    A_FRAMEWORK,
    B_SQLCIPHER_PLAIN,
    C_SQLCIPHER_KEYED,
    D_SQLCIPHER_RAWKEY, // mesma cifra, chave bruta: sem PBKDF2
}

enum class Journal(val mode: JournalMode, val pragma: String) {
    WAL(JournalMode.WRITE_AHEAD_LOGGING, "WAL"),
    TRUNCATE(JournalMode.TRUNCATE, "TRUNCATE"),
}

object DatabaseFactory {

    init {
        System.loadLibrary("sqlcipher")
    }

    fun dbFile(context: Context, arm: Arm): File =
        context.getDatabasePath("bench_${arm.name.lowercase()}.db")

    fun wipe(context: Context, arm: Arm) {
        val base = dbFile(context, arm)
        listOf("", "-wal", "-shm", "-journal").forEach { File(base.path + it).delete() }
    }

    fun open(
        context: Context,
        arm: Arm,
        journal: Journal,
        passphrase: ByteArray,
    ): BenchDatabase {
        val builder = Room.databaseBuilder(
            context,
            BenchDatabase::class.java,
            dbFile(context, arm).absolutePath,
        ).setJournalMode(journal.mode)

            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    // journal_mode devolve uma linha; por isso query, não execSQL
                    db.query("PRAGMA journal_mode = ${journal.pragma}").use { it.moveToFirst() }
                    db.execSQL("PRAGMA synchronous = FULL")
                }
            })

        when (arm) {
            Arm.A_FRAMEWORK -> Unit
            Arm.B_SQLCIPHER_PLAIN -> builder.openHelperFactory(SupportOpenHelperFactory(ByteArray(0)))
            Arm.C_SQLCIPHER_KEYED -> builder.openHelperFactory(SupportOpenHelperFactory(passphrase))
            Arm.D_SQLCIPHER_RAWKEY -> builder.openHelperFactory(SupportOpenHelperFactory(rawKey(passphrase)))
        }
        return builder.build()
    }

    /** Converte 32 bytes no formato de chave bruta do SQLCipher: x'<64 hex>' */
    private fun rawKey(key: ByteArray): ByteArray {
        require(key.size == 32) { "Chave bruta precisa de 32 bytes, recebeu ${key.size}" }
        val hex = key.joinToString("") { "%02x".format(it) }
        return "x'$hex'".toByteArray(Charsets.US_ASCII)
    }
}