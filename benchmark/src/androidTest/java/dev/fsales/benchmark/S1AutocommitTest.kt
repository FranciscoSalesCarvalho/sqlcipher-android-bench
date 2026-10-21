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
class S1AutocommitTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val tag = "S1"
    private val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }

    private val rows = 1_000
    private val payloadBytes = 100
    private val rounds = 5

    @Test
    fun autocommit100bTruncate() = runS1(Journal.TRUNCATE)

    @Test
    fun autocommit100bWal() = runS1(Journal.WAL)

    private fun runS1(journal: Journal) {
        val logical = rows.toLong() * payloadBytes
        Log.i(tag, "header,round,arm,journal,journal_actual,synchronous,rows,payload,logical_bytes," +
                "sectors,bytes,writes,flushes,duration_ms,ms_per_commit,idle_sectors,file_bytes,amplification")

        for (round in 0 until rounds) {
            for (arm in Arm.entries.shuffled(Random(2000 + round))) {
                DatabaseFactory.wipe(context, arm)
                val data = List(rows) { i ->
                    BenchRow(label = "row-$i", payload = Random(round * 31 + i).nextBytes(payloadBytes))
                }

                val store = BenchStore.open(context, arm, journal, passphrase)
                store.warmUp()

                // Confere a configuração real antes de medir
                val journalActual = store.scalar("PRAGMA journal_mode")
                val synchronous = store.scalar("PRAGMA synchronous")

                var durationNs = 0L
                val d = DiskStatsReader.measure {
                    val t0 = SystemClock.elapsedRealtimeNanos()
                    for (row in data) store.insertOne(row) // um commit por linha
                    store.checkpoint()                    // em WAL, copia o -wal para o banco
                    durationNs = SystemClock.elapsedRealtimeNanos() - t0
                }
                check(store.count() == rows) { "$arm inseriu ${store.count()} linhas, esperava $rows" }
                val fileBytes = store.sizeOnDisk()
                store.close()

                val idle = DiskStatsReader.measure {
                    SystemClock.sleep((durationNs / 1_000_000).coerceAtLeast(1))
                }

                val durationMs = durationNs / 1_000_000.0
                Log.i(tag, listOf(
                    "row", round, arm, journal, journalActual, synchronous,
                    rows, payloadBytes, logical,
                    d.sectorsWritten, d.bytesWritten, d.writesCompleted, d.flushes,
                    "%.0f".format(durationMs), "%.3f".format(durationMs / rows),
                    idle.sectorsWritten, fileBytes,
                    "%.1f".format(d.bytesWritten.toDouble() / logical),
                ).joinToString(","))
            }
        }
    }
}