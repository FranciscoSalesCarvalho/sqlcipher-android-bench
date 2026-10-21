package dev.fsales.storage

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.fsales.storage.db.Arm
import dev.fsales.storage.db.BenchRow
import dev.fsales.storage.db.DatabaseFactory
import dev.fsales.storage.db.Journal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.DataInputStream
import java.security.SecureRandom

@RunWith(AndroidJUnit4::class)
class HeaderValidationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val plainHeader = "SQLite format 3\u0000"
    private val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }

    private fun headerOf(arm: Arm): String {
        DatabaseFactory.wipe(context, arm)

        val db = DatabaseFactory.open(context, arm, Journal.TRUNCATE, passphrase)
        db.dao().insertAll(List(10) { BenchRow(label = "row-$it", payload = ByteArray(100)) })
        db.close()

        val header = ByteArray(16)
        DataInputStream(DatabaseFactory.dbFile(context, arm).inputStream()).use { it.readFully(header) }

        Log.i("HeaderValidation", "$arm: ${header.joinToString(" ") { "%02x".format(it) }}")
        return String(header, Charsets.ISO_8859_1)
    }

    @Test
    fun armA_isPlaintext() {
        assertEquals(plainHeader, headerOf(Arm.A_FRAMEWORK))
    }

    @Test
    fun armB_isPlaintext() {
        assertEquals(
            "Braço B não gravou em claro: o controle não é válido",
            plainHeader,
            headerOf(Arm.B_SQLCIPHER_PLAIN),
        )
    }

    @Test
    fun armC_isEncrypted() {
        assertNotEquals(plainHeader, headerOf(Arm.C_SQLCIPHER_KEYED))
    }

    @Test
    fun armB_isReadableByFrameworkSqlite() {
        headerOf(Arm.B_SQLCIPHER_PLAIN) // cria o banco com 10 linhas

        val path = DatabaseFactory.dbFile(context, Arm.B_SQLCIPHER_PLAIN).path
        android.database.sqlite.SQLiteDatabase
            .openDatabase(path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY)
            .use { db ->
                db.rawQuery("SELECT COUNT(*) FROM bench_row", null).use { c ->
                    c.moveToFirst()
                    assertEquals(10, c.getInt(0))
                }
            }
    }
}