package github.boxiaolanya2008.lingxihook.hook

import android.content.Context
import android.content.Intent
import android.util.Log
import github.boxiaolanya2008.lingxihook.LogReceiver
import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.data.LogRepo

/**
 * 统一 Hook 日志出口：
 * 1. 始终输出到 logcat（TAG = LingXiHook，实时调试用）；
 * 2. 本模块自身进程：直接落盘到内部存储（filesDir/logs/lingxi.log）；
 * 3. 目标应用进程（SELinux 限制无法写本应用存储）：通过广播回传给
 *    LogReceiver，由模块应用落盘——日志页可直接查看，不再只能靠 logcat。
 *
 * 高频日志（如类加载监控）请传 persist = false，仅输出 logcat，避免广播风暴。
 */
object HookLogger {

    /** 本模块自身进程的 Context（由 LingXiHook 注入自身时设置） */
    @Volatile
    var ownContext: Context? = null

    fun log(level: LogLevel, tag: String, message: String, persist: Boolean = true) {
        Log.println(priority(level), LingXiHook.TAG, "[$tag] $message")
        if (!persist) return
        val own = ownContext
        if (own != null) {
            LogRepo.append(own, level, tag, message)
            return
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
    }

    private fun priority(level: LogLevel): Int = when (level) {
        LogLevel.INFO -> Log.INFO
        LogLevel.WARN -> Log.WARN
        LogLevel.ERROR -> Log.ERROR
    }
}