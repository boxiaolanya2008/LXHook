package github.boxiaolanya2008.lingxihook.hook

import android.app.Application
import android.provider.Settings

/**
 * Hook 功能开关读取（目标进程侧）。
 *
 * 目标应用进程无法读取本应用的私有 SharedPreferences，因此开关状态由模块应用
 * 写入 Settings.System（需“修改系统设置”权限），Hook 侧在此读取——系统应用可自由读取。
 * 拿不到 ContentResolver 或键不存在时使用默认值（默认开启）。
 */
object HookConfig {

    @Volatile
    private var cachedApp: Application? = null

    /** 反射获取当前进程 Application（ActivityThread.currentApplication） */
    fun currentApplication(): Application? {
        cachedApp?.let { return it }
        val app = runCatching {
            Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null) as? Application
        }.getOrNull()
        if (app != null) cachedApp = app
        return app
    }

    fun isEnabled(key: String, def: Boolean): Boolean {
        val app = currentApplication() ?: return def
        return runCatching {
            Settings.System.getInt(app.contentResolver, key, if (def) 1 else 0) == 1
        }.getOrDefault(def)
    }
}