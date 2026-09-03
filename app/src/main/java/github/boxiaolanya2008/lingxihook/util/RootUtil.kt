package github.boxiaolanya2008.lingxihook.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RootUtil {
    suspend fun isRooted(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val out = p.inputStream.bufferedReader().readText()
            p.waitFor()
            p.exitValue() == 0 && out.contains("uid=0")
        }.getOrDefault(false)
    }

    suspend fun execSetprop(key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val cmd = if (value.isEmpty()) "setprop $key \"\"" else "setprop $key $value"
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            p.waitFor() == 0
        }.getOrDefault(false)
    }

    /** 通用 Root 命令（调用方已切 IO 线程亦可直接调） */
    suspend fun su(cmd: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            p.waitFor() == 0
        }.getOrDefault(false)
    }
}
