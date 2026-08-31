package github.boxiaolanya2008.lingxihook.hook

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.data.LogRepo
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * 模块入口。只做“按包名分发”，不写任何具体 Hook 逻辑：
 * 新增适配 = 写一个 AppHooker 实现类 + 在 HookRegistry 注册一行。
 */
class LingXiHook : XposedModule() {

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        super.onModuleLoaded(param)
        // 警告：改了类名或包名必须同步改 resources/META-INF/xposed/java_init.list，否则框架找不到入口，模块会静默失效
        log(Log.INFO, TAG, "loaded in ${param.processName}, framework=${getFrameworkName()}/${getFrameworkVersion()}")
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        super.onPackageLoaded(param)
        log(Log.INFO, TAG, "package loaded: ${param.packageName}")

        when (val hooker = HookRegistry.find(param.packageName)) {
            null -> if (param.packageName == APP_PACKAGE) markActivated(param)
            else -> runCatching { hooker.install(this, param) }
                .onFailure { log(Log.WARN, TAG, "install ${hooker.packageName} failed: $it") }
        }
    }

    /**
     * 注入自身进程时写激活标记，供主界面显示“已激活”。
     * 双保险：Application.onCreate（最早时机）+ Activity.onCreate（兜底），
     * 避免单一 Hook 点错过导致主页误显示“未检测到注入”。
     */
    private fun markActivated(param: PackageLoadedParam) {
        runCatching {
            val appClass = Class.forName("android.app.Application", false, param.defaultClassLoader)
            hook(appClass.getDeclaredMethod("onCreate"))
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    (chain.thisObject as? Application)?.let { recordActivation(it) }
                    chain.proceed()
                }
        }.onFailure { log(Log.WARN, TAG, "hook Application.onCreate failed: $it") }

        runCatching {
            val activity = Class.forName("android.app.Activity", false, param.defaultClassLoader)
            hook(activity.getDeclaredMethod("onCreate", Bundle::class.java))
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    (chain.thisObject as? Activity)?.let { recordActivation(it) }
                    chain.proceed()
                }
        }.onFailure { log(Log.WARN, TAG, "hook Activity.onCreate failed: $it") }
    }

    private fun recordActivation(context: Context) {
        // 标记本进程为模块自身进程：之后 HookLogger 直接落盘而不再走广播
        HookLogger.ownContext = context.applicationContext
        runCatching {
            context.getSharedPreferences(STATUS_PREF, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_LOADED, System.currentTimeMillis())
                .apply()
            HookLogger.log(LogLevel.INFO, "激活检测", "模块已注入本应用进程")
        }
    }

    companion object {
        const val TAG = "LingXiHook"

        /** 本模块应用包名（自身作用域仅用于激活状态自检） */
        const val APP_PACKAGE = "github.boxiaolanya2008.lingxihook"

        /** 激活状态存储：Hook 端写入，主界面读取 */
        const val STATUS_PREF = "lingxi_status"
        const val KEY_LAST_LOADED = "last_loaded_at"
    }
}
