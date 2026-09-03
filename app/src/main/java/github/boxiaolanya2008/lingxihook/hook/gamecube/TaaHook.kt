package github.boxiaolanya2008.lingxihook.hook.gamecube

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * TAA / 抗锯齿选项强制开启（Hook 点来自 jadx 反编译 com.vivo.gamecube 14.0.15）。
 *
 * 判定链（三处硬编码游戏名单 + 设备位）：
 * - e0#F0(String) = a.p().f0()（画质引擎版本=="q3.0"）且（原神四渠道 || 星铁三渠道），
 *   TAA 底部描述/选项的总闸，QSuperResolutionView/QSuperFrameView/frameinterpolation.h 等引用；
 * - e0#t0(String) = 绝区零三渠道包名硬编码；e0#B0(String) = 鸣潮两渠道包名硬编码，
 *   抗锯齿 no_sdk 文案分支（f0() 且 t0/B0）。
 * V2520A 非 q3.0 且不在任何名单 → TAA/抗锯齿选项恒不显示。
 *
 * 连带校验：F0 放行后，C1() 切挡会对所有游戏激活渲染精度校验
 * （e0#l0：原神要求档位以"3"开头、星铁要"4"，读不到/对不上即 Toast“请将渲染精度
 * 调至高”并把挡位弹回），非旗舰读不到实时值 → 恒拦截。l0 一并强制 true。
 *
 * 方案：已开启时四个方法全部强制 true，任意游戏放出 TAA/抗锯齿选项且切挡不被拦。
 * 关闭开关走原逻辑；PROTECTIVE 容错。
 */
class TaaHook {

    fun install(module: XposedModule, param: PackageLoadedParam) {
        val loader = param.defaultClassLoader
        val utils = runCatching { Class.forName(CLASS_UTILS, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_UTILS not found: $it")
            return
        }
        var hooked = 0
        for (name in METHOD_GATES) {
            val m = utils.declaredMethods.firstOrNull {
                it.name == name && it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java
            } ?: run {
                HookLogger.log(LogLevel.WARN, TAG, "${utils.name}#$name(String) not found")
                continue
            }
            runCatching {
                module.hook(m)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept { chain ->
                        if (!HookConfig.isEnabled(VivoGameCubeHook.FEATURE_TAA, true)) return@intercept chain.proceed()
                        val pkg = chain.args.getOrNull(0) as? String
                        HookLogger.log(LogLevel.INFO, TAG, "$name($pkg) -> true（TAA/抗锯齿放行）", persist = false)
                        true
                    }
                HookLogger.log(LogLevel.INFO, TAG, "hooked ${utils.name}#$name -> true")
                hooked++
            }.onFailure {
                HookLogger.log(LogLevel.WARN, TAG, "hook ${utils.name}#$name failed: $it")
            }
        }
        // l0(String,String)：渲染精度校验结果，强制 true（切挡不再被“请调至高”拦截）
        val l0 = utils.declaredMethods.firstOrNull {
            it.name == METHOD_ACC_CHECK && it.parameterTypes.size == 2 &&
                it.parameterTypes[0] == String::class.java && it.parameterTypes[1] == String::class.java
        }
        if (l0 == null) {
            HookLogger.log(LogLevel.WARN, TAG, "${utils.name}#$METHOD_ACC_CHECK(String,String) not found")
        } else {
            runCatching {
                module.hook(l0)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept { chain ->
                        if (!HookConfig.isEnabled(VivoGameCubeHook.FEATURE_TAA, true)) return@intercept chain.proceed()
                        val pkg = chain.args.getOrNull(1) as? String
                        HookLogger.log(LogLevel.INFO, TAG, "$METHOD_ACC_CHECK($pkg) -> true（渲染精度校验放行）", persist = false)
                        true
                    }
                HookLogger.log(LogLevel.INFO, TAG, "hooked ${utils.name}#$METHOD_ACC_CHECK -> true")
                hooked++
            }.onFailure {
                HookLogger.log(LogLevel.WARN, TAG, "hook ${utils.name}#$METHOD_ACC_CHECK failed: $it")
            }
        }
        if (hooked > 0) {
            HookLogger.log(LogLevel.INFO, TAG, "installed ($hooked hooks) taa unlocked")
        }
    }

    private companion object {
        const val TAG = "taa"
        const val CLASS_UTILS = "com.vivo.gameassistant.utils.e0"
        val METHOD_GATES = arrayOf("F0", "t0", "B0")
        const val METHOD_ACC_CHECK = "l0"
    }
}
