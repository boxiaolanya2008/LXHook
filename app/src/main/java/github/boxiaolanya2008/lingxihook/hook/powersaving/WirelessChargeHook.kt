package github.boxiaolanya2008.lingxihook.hook.powersaving

import android.content.Context
import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * 无线充电适配（Hook 点来自 jadx 反编译 com.iqoo.powersaving 源码，非猜测）。
 *
 * 检测点：com.iqoo.powersaving.utils.g（日志 TAG "BatteryChargeUtils"）
 * - E()：SystemProperties.getBoolean("persist.vivo.wireless_charge_support", false)
 *   无线充电支持总开关。为 false 时 b1/c.java（电池健康与充电设置页 Fragment）会
 *   removePreference("preference_reverse_charge")，反向无线充电开关整个消失。
 * - F(Context)：Settings.Secure "wireless_position_support"==1 && E()
 *   无线充电摆放位置入口。为 false 时 removePreference("preference_wireless_position")。
 *
 * 方案：把 E() / F() 强制返回 true，入口不消失、界面可正常跳转。
 * 开关可在模块「主页 → 省电管理 → 无线充电适配」中控制：
 * 目标进程通过 HookConfig 读取 Settings.System 中的镜像值，默认开启。
 *
 * 注意：E / F 是混淆后方法名，随系统应用版本可能变化；找不到类/方法会打 WARN 日志，
 * 用新版本反编译结果更新下面的常量即可。
 */
class WirelessChargeHook {

    fun install(module: XposedModule, param: PackageLoadedParam) {
        val utils = runCatching {
            Class.forName(CLASS_UTILS, false, param.defaultClassLoader)
        }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_UTILS not found: $it")
            return
        }
        forceTrue(module, utils, METHOD_SUPPORT, emptyArray())
        forceTrue(module, utils, METHOD_POSITION, arrayOf(Context::class.java))
    }

    private fun forceTrue(module: XposedModule, clazz: Class<*>, name: String, params: Array<Class<*>>) {
        val method = runCatching { clazz.getDeclaredMethod(name, *params) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "${clazz.name}#$name not found: $it")
            return
        }
        runCatching {
            module.hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    // 功能开关关闭时保持原逻辑
                    if (!HookConfig.isEnabled(IqooPowerSavingHook.FEATURE_WIRELESS, true)) {
                        return@intercept chain.proceed()
                    }
                    val origin = chain.proceed()
                    if (origin != true) {
                        HookLogger.log(
                            LogLevel.INFO, TAG,
                            "${clazz.simpleName}#$name: $origin -> true（入口/功能已放行）"
                        )
                    }
                    true
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#$name -> true")
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook ${clazz.name}#$name failed: $it")
        }
    }

    private companion object {
        const val TAG = "wireless"
        const val CLASS_UTILS = "com.iqoo.powersaving.utils.g"
        const val METHOD_SUPPORT = "E"
        const val METHOD_POSITION = "F"
    }
}