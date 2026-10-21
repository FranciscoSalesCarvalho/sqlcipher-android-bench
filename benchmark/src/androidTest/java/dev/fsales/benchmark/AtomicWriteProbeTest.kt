package dev.fsales.benchmark

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.fsales.storage.db.Arm
import dev.fsales.storage.db.BenchRow
import dev.fsales.storage.db.BenchStore
import dev.fsales.storage.db.DatabaseFactory
import dev.fsales.storage.db.Journal
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.SecureRandom

@RunWith(AndroidJUnit4::class)
class AtomicWriteProbeTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val tag = "Atomic"
    private val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }

    private val interesting = listOf("ATOMIC", "SYNCHRONOUS", "JOURNAL", "WAL", "CODEC", "PAGE_SIZE")

    @Test
    fun probe() {
        Arm.entries.forEach { arm ->
            DatabaseFactory.wipe(context, arm)

            // 1ª abertura: o Room cria as tabelas (isso pode criar o journal)
            BenchStore.open(context, arm, Journal.TRUNCATE, passphrase).use { store ->
                store.warmUp()
                if (arm == Arm.A_FRAMEWORK || arm == Arm.B_SQLCIPHER_PLAIN) {
                    val options = store.column("PRAGMA compile_options")
                    Log.i(tag, "$arm compile_options (${options.size}): ${options.joinToString(" ")}")
                }
            }

            // Banco fechado: removemos o journal deixado pela criação
            val journal = File(DatabaseFactory.dbFile(context, arm).path + "-journal")
            journal.delete()

            // 2ª abertura: tabelas já existem, nenhuma transação de criação
            BenchStore.open(context, arm, Journal.TRUNCATE, passphrase).use { store ->
                store.warmUp()
                Log.i(tag, "$arm após reabrir: journal existe=${journal.exists()}")

                repeat(10) { i -> store.insertOne(BenchRow(label = "probe-$i", payload = ByteArray(100))) }

                Log.i(tag, "$arm depois de 10 commits: journal existe=${journal.exists()} " +
                        "tamanho=${if (journal.exists()) journal.length() else -1}")
            }
        }
    }
}