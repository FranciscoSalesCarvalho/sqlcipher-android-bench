package dev.fsales.benchmark

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.fsales.benchmark.metrics.DiskStatsReader
import dev.fsales.storage.db.Arm
import dev.fsales.storage.db.BenchRow
import dev.fsales.storage.db.BenchStore
import dev.fsales.storage.db.DatabaseFactory
import dev.fsales.storage.db.Journal
import org.junit.Test
import org.junit.runner.RunWith
import java.security.SecureRandom
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class S2FirstLookTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val tag = "S2"
    private val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }

    private val journal = Journal.TRUNCATE
    private val rounds = 5

    @Test
    fun pragmasPerArm() {
        val queries = listOf(
            "PRAGMA journal_mode",
            "PRAGMA synchronous",
            "PRAGMA page_size",
            "PRAGMA cipher_version",
            "PRAGMA cipher_page_size",
            "SELECT sqlite_version()",
        )
        Arm.entries.forEach { arm ->
            DatabaseFactory.wipe(context, arm)
            BenchStore.open(context, arm, journal, passphrase).use { store ->
                store.warmUp()
                val values = queries.joinToString(" | ") { "$it=${store.scalar(it)}" }
                Log.i(tag, "PRAGMA $arm $values")
            }
        }
    }

    @Test
    fun batchInsert4k() = runS2(rows = 2_000, payloadBytes = 4_096)

    @Test
    fun batchInsert100b() = runS2(rows = 10_000, payloadBytes = 100)

    private fun runS2(rows: Int, payloadBytes: Int) {
        val logical = rows.toLong() * payloadBytes
        Log.i(tag, "header,round,arm,rows,payload,logical_bytes,sectors,bytes,writes,duration_ms," +
                "idle_sectors,idle_bytes,file_bytes,amplification")

        for (round in 0 until rounds) {
            for (arm in Arm.entries.shuffled(Random(1000 + round))) {
                DatabaseFactory.wipe(context, arm)

                // Dados gerados fora da medição, com seeds fixos:
                // os três braços de uma rodada gravam exatamente os mesmos bytes.
                val data = List(rows) { i ->
                    BenchRow(label = "row-$i", payload = Random(round * 31 + i).nextBytes(payloadBytes))
                }

                val store = BenchStore.open(context, arm, journal, passphrase)
                store.warmUp() // abertura e derivação de chave fora da medição

                var durationNs = 0L
                val d = DiskStatsReader.measure {
                    val t0 = SystemClock.elapsedRealtimeNanos()
                    store.insertBatch(data)
                    store.checkpoint()
                    durationNs = SystemClock.elapsedRealtimeNanos() - t0
                }

                // Confere que o cenário fez o que devia (fora da medição)
                check(store.count() == rows) { "$arm inseriu ${store.count()} linhas, esperava $rows" }
                val fileBytes = store.sizeOnDisk()
                store.close()

                // Janela ociosa pareada: mesmo tempo, sem fazer nada
                val idle = DiskStatsReader.measure {
                    SystemClock.sleep((durationNs / 1_000_000).coerceAtLeast(1))
                }

                val amplification = d.bytesWritten.toDouble() / logical
                Log.i(tag, listOf(
                    "row", round, arm, rows, payloadBytes, logical,
                    d.sectorsWritten, d.bytesWritten, d.writesCompleted, durationNs / 1_000_000,
                    idle.sectorsWritten, idle.bytesWritten, fileBytes, "%.3f".format(amplification),
                ).joinToString(","))
            }
        }
    }
}