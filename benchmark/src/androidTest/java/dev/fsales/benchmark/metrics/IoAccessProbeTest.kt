package dev.fsales.benchmark.metrics

import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.Process
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IoAccessProbeTest {

    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

    private fun ParcelFileDescriptor.readAll(): String =
        ParcelFileDescriptor.AutoCloseInputStream(this).bufferedReader().use { it.readText() }

    /** Executa sem shell: sem aspas, pipes ou redirecionamentos. Devolve stdout e stderr. */
    private fun shell(cmd: String): String {
        if (Build.VERSION.SDK_INT >= 31) {
            val (out, stdin, err) = uiAutomation.executeShellCommandRwe(cmd)
            stdin.close()
            val stdout = out.readAll()
            val stderr = err.readAll()
            return if (stderr.isBlank()) stdout else "$stdout\n[stderr] $stderr"
        }
        return uiAutomation.executeShellCommand(cmd).readAll()
    }

    @Test
    fun probe() {
        val pid = Process.myPid()
        Log.i("IoProbe", "SDK=${Build.VERSION.SDK_INT} pid=$pid modelo=${Build.MODEL}")

        val probes = listOf(
            "id do shell" to "id",
            "io do app via shell" to "cat /proc/$pid/io",
            "diskstats" to "cat /proc/diskstats",
            "dispositivos de bloco" to "ls /sys/block",
        )
        probes.forEach { (name, cmd) ->
            Log.i("IoProbe", "===== $name ($cmd)\n${shell(cmd)}")
        }

        val dataMount = shell("cat /proc/mounts").lines().filter { " /data " in it }
        Log.i("IoProbe", "===== /data montado em\n${dataMount.joinToString("\n")}")
    }
}
