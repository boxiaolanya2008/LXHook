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
 * 日志持久化：保存到应用内部存储 filesDir/logs/lingxi.log
 *
 * 说明：Hook 代码运行在目标应用进程（如 com.iqoo.powersaving）时受 SELinux 限制，
 * 无法写入本应用内部存储，那些日志只走 logcat（adb logcat -s LingXiHook）；
 * 本应用自身进程（激活检测、UI 事件）的日志会落盘，供“日志”页展示。
 */
object LogRepo {
    private const val DIR = "logs"
    private const val FILE = "lingxi.log"

    fun append(context: Context, level: LogLevel, tag: String, message: String) {
        runCatching {
            val dir = File(context.filesDir, DIR).apply { mkdirs() }
            File(dir, FILE).appendText("${System.currentTimeMillis()}|$level|$tag|$message\n")
        }
    }

    fun readAll(context: Context): List<LogEntry> = runCatching {
        val file = File(File(context.filesDir, DIR), FILE)
        if (!file.exists()) {
            emptyList()
        } else {
            file.readLines().mapNotNull { line ->
                val parts = line.split("|", limit = 4)
                if (parts.size < 4) return@mapNotNull null
                LogEntry(
                    time = parts[0].toLongOrNull() ?: 0L,
                    level = LogLevel.valueOf(parts[1]),
                    tag = parts[2],
                    message = parts[3]
                )
            }.sortedByDescending { it.time }
        }
    }.getOrDefault(emptyList())

    fun clear(context: Context) {
        runCatching { File(File(context.filesDir, DIR), FILE).delete() }
    }
}