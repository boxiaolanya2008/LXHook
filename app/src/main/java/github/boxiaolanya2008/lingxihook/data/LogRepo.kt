package github.boxiaolanya2008.lingxihook.data

import android.content.Context
import java.io.File

/** 日志等级（分类过滤用） */
enum class LogLevel { INFO, WARN, ERROR }

data class LogEntry(
    val time: Long,
    val level: LogLevel,
    val tag: String,
    val message: String
)

/**
 * 日志持久化：双文件合并
 * - 内部存储 filesDir/logs/lingxi.log（自身进程直写）
 * - 共享文件 /data/local/tmp/lingxihook.log（目标进程 world-writable 直写，绕过广播限）
 * 日志页合并两文件按时间倒序，clear 时双清。
 */
object LogRepo {
    private const val DIR = "logs"
    private const val FILE = "lingxi.log"
    const val TMP_FILE = "/data/local/tmp/lingxihook.log"

    fun append(context: Context, level: LogLevel, tag: String, message: String) {
        runCatching {
            val dir = File(context.filesDir, DIR).apply { mkdirs() }
            File(dir, FILE).appendText("${System.currentTimeMillis()}|$level|$tag|$message\n")
        }
    }

    fun appendTmp(level: LogLevel, tag: String, message: String) {
        runCatching {
            File(TMP_FILE).appendText("${System.currentTimeMillis()}|$level|$tag|$message\n")
            runCatching { Runtime.getRuntime().exec(arrayOf("chmod", "666", TMP_FILE)) }
        }
    }

    private fun parseFile(file: File): List<LogEntry> = runCatching {
        if (!file.exists()) return emptyList()
        file.readLines().mapNotNull { line ->
            val parts = line.split("|", limit = 4)
            if (parts.size < 4) return@mapNotNull null
            LogEntry(
                time = parts[0].toLongOrNull() ?: 0L,
                level = LogLevel.valueOf(parts[1]),
                tag = parts[2],
                message = parts[3]
            )
        }
    }.getOrDefault(emptyList())

    fun readAll(context: Context): List<LogEntry> = runCatching {
        val internalFile = File(File(context.filesDir, DIR), FILE)
        val tmpFile = File(TMP_FILE)
        (parseFile(internalFile) + parseFile(tmpFile)).sortedByDescending { it.time }
    }.getOrDefault(emptyList())

    fun readTmpOnly(): List<LogEntry> = parseFile(File(TMP_FILE)).sortedByDescending { it.time }

    fun clear(context: Context) {
        runCatching { File(File(context.filesDir, DIR), FILE).delete() }
        runCatching { File(TMP_FILE).delete() }
    }

    fun clearTmpOnly() {
        runCatching { File(TMP_FILE).delete() }
    }
}