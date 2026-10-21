package dev.fsales.benchmark.metrics

import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry

object Shell {
    private val uiAutomation get() = InstrumentationRegistry.getInstrumentation().uiAutomation

    private fun ParcelFileDescriptor.readAll(): String =
        ParcelFileDescriptor.AutoCloseInputStream(this).bufferedReader().use { it.readText() }

    /** Executa como usuário shell. Sem aspas, pipes ou redirecionamentos. */
    fun run(cmd: String): String {
        require(Build.VERSION.SDK_INT >= 31) { "Precisa de API 31+ para capturar stderr" }
        val (out, stdin, err) = uiAutomation.executeShellCommandRwe(cmd)
        stdin.close()
        val stdout = out.readAll()
        val stderr = err.readAll()
        check(stderr.isBlank()) { "'$cmd' falhou: $stderr" }
        return stdout
    }
}
