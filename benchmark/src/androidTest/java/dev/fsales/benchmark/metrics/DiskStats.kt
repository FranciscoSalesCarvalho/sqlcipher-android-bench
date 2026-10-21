package dev.fsales.benchmark.metrics

data class DiskStats(
    val device: String,
    val writesCompleted: Long,
    val sectorsWritten: Long,
    val sectorsDiscarded: Long,
    val flushes: Long,
) {
    val bytesWritten: Long get() = sectorsWritten * SECTOR_BYTES

    operator fun minus(before: DiskStats): DiskStats {
        require(device == before.device) { "Dispositivos diferentes: $device vs ${before.device}" }
        return DiskStats(
            device = device,
            writesCompleted = writesCompleted - before.writesCompleted,
            sectorsWritten = sectorsWritten - before.sectorsWritten,
            sectorsDiscarded = sectorsDiscarded - before.sectorsDiscarded,
            flushes = flushes - before.flushes,
        )
    }

    companion object {
        const val SECTOR_BYTES = 512L
    }
}

object DiskStatsReader {

    /** Nome do dispositivo de bloco que monta /data, por exemplo "dm-65". */
    val dataDevice: String by lazy {
        val line = Shell.run("cat /proc/mounts").lines()
            .firstOrNull { it.split(' ').getOrNull(1) == "/data" }
            ?: error("/data não encontrado em /proc/mounts")
        line.substringBefore(' ').substringAfterLast('/')
    }

    fun take(device: String = dataDevice): DiskStats {
        val parts = Shell.run("cat /proc/diskstats").lines()
            .map { it.trim().split(Regex("\\s+")) }
            .firstOrNull { it.getOrNull(2) == device }
            ?: error("Dispositivo '$device' não está em /proc/diskstats")

        val f = parts.drop(3).map { it.toLong() } // f[0] é o campo 1 da tabela
        check(f.size >= 7) { "Linha inesperada para $device: $parts" }

        return DiskStats(
            device = device,
            writesCompleted = f[4],
            sectorsWritten = f[6],
            sectorsDiscarded = f.getOrElse(13) { 0 },
            flushes = f.getOrElse(15) { 0 },
        )
    }

    fun sync() {
        Shell.run("sync")
    }

    /** sync → foto → bloco → sync → foto. Devolve o delta. */
    inline fun measure(block: () -> Unit): DiskStats {
        sync()
        val before = take()
        block()
        sync()
        return take() - before
    }
}