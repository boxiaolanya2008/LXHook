package github.boxiaolanya2008.lingxihook.hook.gamecube

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * 显示设置强制开启（Hook 点来自 jadx 反编译 com.vivo.gamecube 14.0.15，非猜测）。
 *
 * 背景与两个判定点：
 * - 入口总开关 e0#T0() = !海外(c0) && !O1() && j0(DISPLAY_SETTINGS)，而
 *   j0 → ha.b#i → 设备级 hardware_dimen 机型白名单（display_settings 仅列到
 *   PD2254 等旧机型），PD2520/V2520A 不在列，T0 恒 false，入口整个消失。
 *   → 强制 T0()=true 一步到位。
 * - h0 总闸（其余经 h0(DISPLAY_SETTINGS) 的路径）对 display_settings 同样强制 true 兜底。
 *
 * 关闭开关走原逻辑；所有拦截均为 PROTECTIVE 容错。
 */
class DisplaySettingsHook {

    fun install(module: XposedModule, param: PackageLoadedParam) {
        val loader = param.defaultClassLoader
        var hooked = 0
        hooked += hookEntrySwitch(module, loader)
        hooked += hookGate(module, loader)
        if (hooked == 0) {
            HookLogger.log(LogLevel.WARN, TAG, "no display settings methods hooked")
        } else {
            HookLogger.log(LogLevel.INFO, TAG, "installed ($hooked hooks) display settings unlocked")
        }
    }

    /** 入口总开关 e0#T0() 强制 true */
    private fun hookEntrySwitch(module: XposedModule, loader: ClassLoader): Int {
        val utils = runCatching { Class.forName(CLASS_UTILS, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_UTILS not found: $it")
            return 0
        }
        val t0 = utils.declaredMethods.firstOrNull {
            it.name == METHOD_ENTRY && it.parameterTypes.isEmpty()
        } ?: run {
            HookLogger.log(LogLevel.WARN, TAG, "${utils.name}#$METHOD_ENTRY() not found")
            return 0
        }
        return runCatching {
            module.hook(t0)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (!HookConfig.isEnabled(VivoGameCubeHook.FEATURE_DISPLAY, true)) return@intercept chain.proceed()
                    HookLogger.log(LogLevel.INFO, TAG, "$METHOD_ENTRY -> true（显示设置入口放行）", persist = false)
                    true
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${utils.name}#$METHOD_ENTRY -> true")
            1
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook ${utils.name}#$METHOD_ENTRY failed: $it")
        }.getOrDefault(0)
    }

    /** h0 总闸兜底：funcName == display_settings 强制 true */
    private fun hookGate(module: XposedModule, loader: ClassLoader): Int {
        val utils = runCatching { Class.forName(CLASS_UTILS, false, loader) }.getOrElse { return 0 }
        val cfg = runCatching { Class.forName(CLASS_CONFIGURED_FUNCTION, false, loader) }.getOrElse { return 0 }
        val gate = utils.declaredMethods.firstOrNull {
            it.name == METHOD_GATE && it.parameterTypes.size == 2 &&
                it.parameterTypes[0] == cfg && it.parameterTypes[1] == String::class.java
        } ?: run {
            HookLogger.log(LogLevel.WARN, TAG, "${utils.name}#$METHOD_GATE(ConfiguredFunction,String) not found")
            return 0
        }
        return runCatching {
            module.hook(gate)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (!HookConfig.isEnabled(VivoGameCubeHook.FEATURE_DISPLAY, true)) return@intercept chain.proceed()
                    val func = chain.args.getOrNull(0) ?: return@intercept chain.proceed()
                    val name = runCatching {
                        cfg.getMethod(METHOD_FUNC_NAME).invoke(func) as? String
                    }.getOrNull() ?: return@intercept chain.proceed()
                    if (name == FUNC_DISPLAY_SETTINGS) {
                        HookLogger.log(LogLevel.INFO, TAG, "$METHOD_GATE($name) -> true（显示设置判定放行）", persist = false)
                        return@intercept true
                    }
                    chain.proceed()
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${utils.name}#$METHOD_GATE -> display_settings 放行")
            1
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook ${utils.name}#$METHOD_GATE failed: $it")
        }.getOrDefault(0)
    }

    private companion object {
        const val TAG = "display"
        const val CLASS_UTILS = "com.vivo.gameassistant.utils.e0"
        const val CLASS_CONFIGURED_FUNCTION = "com.vivo.common.supportlist.pojo.ConfiguredFunction"
        const val METHOD_ENTRY = "T0"
        const val METHOD_GATE = "h0"
        const val METHOD_FUNC_NAME = "getFuncName"
        const val FUNC_DISPLAY_SETTINGS = "display_settings"
    }
}
