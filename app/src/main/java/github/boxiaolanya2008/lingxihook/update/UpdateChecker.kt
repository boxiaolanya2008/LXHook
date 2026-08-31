package github.boxiaolanya2008.lingxihook.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 无缓存更新检查器。
 * 每次请求强制走网络：Cache-Control: no-cache + Pragma: no-cache + URL 时间戳，
 * 确保 force_update 切换无延迟。
 */
object UpdateChecker {

    private const val DEFAULT_URL = "https://gitee.com/boxiaolanya2008/lxhook-update/raw/master/update.json"

    suspend fun check(
        context: Context,
        remoteUrl: String = DEFAULT_URL
    ): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val noCacheUrl = if (remoteUrl.contains("?")) "$remoteUrl&t=${System.currentTimeMillis()}" else "$remoteUrl?t=${System.currentTimeMillis()}"
            val conn = URL(noCacheUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Cache-Control", "no-cache")
            conn.setRequestProperty("Pragma", "no-cache")
            conn.setRequestProperty("Cache-Control", "no-store")
            conn.useCaches = false
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.connect()
            val code = conn.responseCode
            if (code !in 200..299) error("http $code")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            UpdateInfo.fromJson(body)
        }
    }

    fun currentVersionCode(context: Context): Int {
        return runCatching {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0)).let {
                    it.longVersionCode.toInt()
                }
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0).versionCode
            }
        }.getOrDefault(1)
    }

    fun currentVersionName(context: Context): String {
        return runCatching {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0)).versionName ?: "1.0"
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
            }
        }.getOrDefault("1.0")
    }

    fun openDownload(context: Context, url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
