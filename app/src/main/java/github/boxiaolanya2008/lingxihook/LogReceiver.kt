package github.boxiaolanya2008.lingxihook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.data.LogRepo

/**
 * Hook 日志接收器：目标应用进程（如 com.iqoo.powersaving）通过广播回传的日志
 * 在这里落盘到应用内部存储，“日志”页即可直接查看。
 */
class LogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val level = runCatching {
            LogLevel.valueOf(intent.getStringExtra("level") ?: LogLevel.INFO.name)
        }.getOrDefault(LogLevel.INFO)
        val tag = intent.getStringExtra("tag") ?: "Hook"
        val msg = intent.getStringExtra("msg") ?: ""
        if (msg.isBlank()) return
        LogRepo.append(context, level, tag, msg)
        LogRepo.appendTmp(level, tag, msg)
    }

    companion object {
        const val ACTION = "github.boxiaolanya2008.lingxihook.HOOK_LOG"
    }
}