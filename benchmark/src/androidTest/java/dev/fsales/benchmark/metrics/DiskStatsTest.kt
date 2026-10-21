package dev.fsales.benchmark.metrics

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class DiskStatsTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val tag = "DiskStats"

    private fun log(label: String, d: DiskStats) = Log.i(
        tag,
        "$label device=${d.device} sectors=${d.sectorsWritten} bytes=${d.bytesWritten} " +
                "writes=${d.writesCompleted} flushes=${d.flushes} discarded=${d.sectorsDiscarded}",
    )

    @Test
    fun idleNoise() {
        Log.i(tag, "dataDevice=${DiskStatsReader.dataDevice}")
        repeat(10) { i ->
            val d = DiskStatsReader.measure { Thread.sleep(2_000) }
            log("idle[$i] 2s", d)
        }
    }

    @Test
    fun detectsKnownWrite() {
        val size = 4 * 1024 * 1024
        val expectedSectors = size / DiskStats.SECTOR_BYTES
        val data = Random(42).nextBytes(size)
        val file = File(context.filesDir, "io-probe.bin").apply { delete() }

        repeat(5) { i ->
            val d = DiskStatsReader.measure {
                FileOutputStream(file).use { out ->
                    out.write(data)
                    out.fd.sync()
                }
            }
            log("write4MiB[$i]", d)
            file.delete()
            assertTrue(
                "Esperava >= $expectedSectors setores, medi ${d.sectorsWritten}",
                d.sectorsWritten >= expectedSectors,
            )
        }
    }
}
