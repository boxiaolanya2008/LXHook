package github.boxiaolanya2008.lingxihook.hook

import android.content.Context
import android.content.Intent
import android.util.Log
import github.boxiaolanya2008.lingxihook.LogReceiver
import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.data.LogRepo

/**
 * 统一 Hook 日志出口（重构后不依赖 logcat）：
 * 1. 始终输出到 logcat（TAG = LingXiHook，adb 实时用）；
 * 2. 本模块自身进程：直写内部存储 filesDir/logs/lingxi.log；
 * 3. 目标进程：优先直写 world-writable /data/local/tmp/lingxihook.log（绕过 SELinux 与广播限），
 *    失败再走广播 LogReceiver 兜底，日志页合并双文件无需 adb 即可看 Hook 日志。
 *
 * 高频路径传 persist=false 仅 logcat。
 */
object HookLogger {

    @Volatile
    var ownContext: Context? = null

    fun log(level: LogLevel, tag: String, message: String, persist: Boolean = true) {
        Log.println(priority(level), LingXiHook.TAG, "[$tag] $message")
        if (!persist) return
        val own = ownContext
        if (own != null) {
            LogRepo.append(own, level, tag, message)
            LogRepo.appendTmp(level, tag, message)
            return
        }
        var wroteTmp = false
        runCatching {
            LogRepo.appendTmp(level, tag, message)
            wroteTmp = true
        }
        runCatching {
            val app = HookConfig.currentApplication()
            if (app != null) {
                app.sendBroadcast(
                    Intent(LogReceiver.ACTION)
                        .setPackage(LingXiHook.APP_PACKAGE)
                        .putExtra("level", level.name)
                        .putExtra("tag", tag)
                        .putExtra("msg", message)
                )
            }
        }
        if (!wroteTmp) {
            runCatching {
                java.io.File(LogRepo.TMP_FILE).appendText("${System.currentTimeMillis()}|$level|$tag|$message\n")
            }
        }
    }

    private fun priority(level: LogLevel): Int = when (level) {
        LogLevel.INFO -> Log.INFO
        LogLevel.WARN -> Log.WARN
        LogLevel.ERROR -> Log.ERROR
    }
}