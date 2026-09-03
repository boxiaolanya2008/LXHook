package github.boxiaolanya2008.lingxihook.hook.gamecube

import android.content.Context
import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * 游戏光影追踪（LightTrack / XPQ）入口强制开启（Hook 点来自 jadx 反编译 com.vivo.gamecube 14.0.15）。
 *
 * 判定链：QChipContentView#j 中光追条目显示 = m0.c().f(context, pkg)，其实现读取
 * Settings.Global "xpq_whitelist_apps"（分号分隔的包名白名单），仅官方光追合作游戏
 * 在列（XPQ = X 系列光追增强，底层走 xpq_value_from_gamecube 下发）。
 *
 * 方案：已开启时拦截 m0#f(Context,String) 强制 true，任意游戏放出光追条目。
 * 注意：条目放行后底层 XPQ 服务仍需硬件/系统支持，实际渲染效果以游戏内实测为准。
 * 关闭开关走原逻辑；PROTECTIVE 容错。
 */
class LightTrackHook {

    fun install(module: XposedModule, param: PackageLoadedParam) {
        val loader = param.defaultClassLoader
        val m0 = runCatching { Class.forName(CLASS_LIGHT_TRACK_UTILS, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_LIGHT_TRACK_UTILS not found: $it")
            return
        }
        val f = m0.declaredMethods.firstOrNull {
            it.name == METHOD_IN_WHITELIST && it.parameterTypes.size == 2 &&
                it.parameterTypes[0] == Context::class.java && it.parameterTypes[1] == String::class.java
        } ?: run {
            HookLogger.log(LogLevel.WARN, TAG, "${m0.name}#$METHOD_IN_WHITELIST(Context,String) not found")
            return
        }
        runCatching {
            module.hook(f)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (!HookConfig.isEnabled(VivoGameCubeHook.FEATURE_LIGHT_TRACK, true)) return@intercept chain.proceed()
                    val pkg = chain.args.getOrNull(1) as? String
                    HookLogger.log(LogLevel.INFO, TAG, "$METHOD_IN_WHITELIST($pkg) -> true（光追条目放行）", persist = false)
                    true
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${m0.name}#$METHOD_IN_WHITELIST -> true")
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook ${m0.name}#$METHOD_IN_WHITELIST failed: $it")
        }
    }

    private companion object {
        const val TAG = "lighttrack"
        const val CLASS_LIGHT_TRACK_UTILS = "com.vivo.common.utils.m0"
        const val METHOD_IN_WHITELIST = "f"
    }
}
