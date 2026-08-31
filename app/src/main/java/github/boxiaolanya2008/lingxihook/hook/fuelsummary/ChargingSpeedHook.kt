package github.boxiaolanya2008.lingxihook.hook.fuelsummary

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * 充电限流移除（Hook 点来自 jadx 反编译 com.vivo.fuelsummary 2.5.0.3）。
 *
 * 限速链：
 * - r0.f#K() charging_power_max>=20
 * - r0.f#M(Context) balance_charge_switch
 * - r0.f#H()/I()/J() ai_charge_support
 * - r0.f#y/z(Context) optimize_charge_switch 智能限流开关
 * - r0.f#h() batt_therm_thr 42℃
 * - r0.f#f0()/g0() 高低温停充特性
 * - r0.f#F() charge_capacity_up_limit
 * - r0.f#f(Context) smart_charge_upper_value 100
 * - com.vivo.fuelsummary.h#r(String) echo > sys 节点限流写入
 *
 * 方案：已开启时全部强制放行或抬阈值，关闭走原逻辑。
 */
class ChargingSpeedHook {

    fun install(module: XposedModule, param: PackageLoadedParam) {
        val loader = param.defaultClassLoader
        var hooked = 0
        hooked += hookSupportGates(module, loader)
        hooked += hookThermalAndLimits(module, loader)
        hooked += hookShellWrite(module, loader)
        if (hooked == 0) {
            HookLogger.log(LogLevel.WARN, TAG, "no charging methods hooked, maybe obfuscation changed")
        } else {
            HookLogger.log(LogLevel.INFO, TAG, "installed ($hooked hooks) charging unlimit")
        }
    }

    private fun hookSupportGates(module: XposedModule, loader: ClassLoader): Int {
        val clazz = runCatching { Class.forName(CLASS_BATTERY_UTILS, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_BATTERY_UTILS not found: $it")
            return 0
        }
        var count = 0
        val boolTrue = listOf("K", "M", "H", "I", "J", "L", "E")
        for (name in boolTrue) {
            val methods = clazz.declaredMethods.filter { it.name == name }
            for (m in methods) {
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CHARGING, true)) return@intercept chain.proceed()
                        val origin = chain.proceed() as? Boolean
                        if (origin != true) HookLogger.log(LogLevel.INFO, TAG, "${clazz.simpleName}#$name: $origin -> true (ultrafast/ai true)")
                        true
                    }
                    HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#$name -> true")
                    count++
                }.onFailure { HookLogger.log(LogLevel.WARN, TAG, "hook ${clazz.name}#$name failed: $it") }
            }
        }
        val boolFalse = listOf("y", "z")
        for (name in boolFalse) {
            val methods = clazz.declaredMethods.filter { it.name == name }
            for (m in methods) {
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CHARGING, true)) return@intercept chain.proceed()
                        val origin = chain.proceed() as? Boolean
                        if (origin != false) HookLogger.log(LogLevel.INFO, TAG, "${clazz.simpleName}#$name: $origin -> false (smart off)")
                        false
                    }
                    HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#$name -> false")
                    count++
                }
            }
        }
        runCatching { clazz.getDeclaredMethod("F") }.onSuccess { m ->
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CHARGING, true)) return@intercept chain.proceed()
                    false
                }
                count++
            }
        }
        runCatching { clazz.getDeclaredMethod("f0") }.onSuccess { m ->
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CHARGING, true)) return@intercept chain.proceed()
                    false
                }
                count++
            }
        }
        return count
    }

    private fun hookThermalAndLimits(module: XposedModule, loader: ClassLoader): Int {
        val clazz = runCatching { Class.forName(CLASS_BATTERY_UTILS, false, loader) }.getOrNull() ?: return 0
        var count = 0
        runCatching { clazz.getDeclaredMethod("h") }.onSuccess { m ->
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CHARGING, true)) return@intercept chain.proceed()
                    80
                }
                HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#h -> 80")
                count++
            }
        }
        runCatching { clazz.getDeclaredMethod("f", android.content.Context::class.java) }.onSuccess { m ->
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CHARGING, true)) return@intercept chain.proceed()
                    100
                }
                HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#f(Context) -> 100")
                count++
            }
        }
        runCatching { clazz.getDeclaredMethod("b0", android.content.Intent::class.java) }.onSuccess { m ->
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CHARGING, true)) return@intercept chain.proceed()
                    false
                }
                count++
            }
        }
        runCatching { clazz.getDeclaredMethod("a0") }.onSuccess { m ->
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CHARGING, true)) return@intercept chain.proceed()
                    false
                }
                count++
            }
        }
        count += hookPersistAndThermal(module, loader)
        return count
    }

    private fun hookPersistAndThermal(module: XposedModule, loader: ClassLoader): Int {
        var count = 0
        val qClazz = runCatching { Class.forName("r0.q", false, loader) }.getOrNull()
        if (qClazz != null) {
            for (m in qClazz.declaredMethods.filter { it.name == "d" && it.parameterTypes.size == 2 && it.parameterTypes[0] == String::class.java }) {
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CHARGING, true)) return@intercept chain.proceed()
                        val key = chain.args[0] as? String ?: return@intercept chain.proceed()
                        val origin = chain.proceed() as? Int ?: return@intercept chain.proceed()
                        when {
                            key == "persist.vivo.charging_power_max" -> {
                                val target = 120
                                if (origin != target) HookLogger.log(LogLevel.INFO, TAG, "q#d($key): $origin -> $target")
                                target
                            }
                            key == "persist.chg.reverse_tx.batt_therm_thr" -> 80
                            key == "persist.vivo.charge_capacity_up_limit_support" -> 0
                            key == "persist.vivo.battery_health_support" -> 0
                            else -> origin
                        }
                    }
                    HookLogger.log(LogLevel.INFO, TAG, "hooked r0.q#d -> power/thermal override")
                    count++
                }
            }
            for (m in qClazz.declaredMethods.filter { it.name == "c" && it.parameterTypes.size == 2 }) {
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CHARGING, true)) return@intercept chain.proceed()
                        val key = chain.args[0] as? String ?: return@intercept chain.proceed()
                        if (key == "persist.vivo.wireless_charge_support") return@intercept true
                        chain.proceed()
                    }
                    count++
                }
            }
        }
        val fuelH = runCatching { Class.forName(CLASS_FUEL_H, false, loader) }.getOrNull()
        if (fuelH != null) {
            for (m in fuelH.declaredMethods.filter { it.name == "q" && it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java }) {
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CHARGING, true)) return@intercept chain.proceed()
                        val key = chain.args[0] as? String ?: return@intercept chain.proceed()
                        val lower = key.lowercase()
                        when {
                            lower.startsWith("board_thermal_thresholds") -> 85
                            lower == "fcc_too_low" -> 1000
                            lower == "sf_shutdown_vbat" -> 3200
                            lower == "hw_shutdown_vbat" -> 3200
                            lower == "full_drop_seconds" -> 3600
                            else -> chain.proceed() as? Int ?: 0
                        }
                    }
                    HookLogger.log(LogLevel.INFO, TAG, "hooked h#q -> thermal thresholds 85")
                    count++
                }
            }
            for (m in fuelH.declaredMethods.filter { it.name == "C" && it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java }) {
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CHARGING, true)) return@intercept chain.proceed()
                        val path = chain.args[0] as? String ?: return@intercept chain.proceed()
                        val lower = path.lowercase()
                        if (lower.contains("thermal") || lower.contains("temp")) {
                            val origin = chain.proceed() as? Int ?: 0
                            if (origin < 85) return@intercept 85
                        }
                        chain.proceed()
                    }
                    count++
                }
            }
            for (m in fuelH.declaredMethods.filter { it.name == "r" && it.parameterTypes.size == 1 }) {
                // 已在 hookShellWrite 处理 fex，此处追加旁路直充标记
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CHARGING, true)) return@intercept chain.proceed()
                        val cmd = chain.args[0] as? String ?: return@intercept chain.proceed()
                        if (cmd.contains("bypass", true) || cmd.contains("game_bypass")) {
                            HookLogger.log(LogLevel.INFO, TAG, "h#r bypass blocked: $cmd")
                            return@intercept ""
                        }
                        chain.proceed()
                    }
                    count++
                }
            }
        }
        val vivoDm = runCatching { Class.forName("com.vivo.services.daemon.VivoDmServiceProxy", false, loader) }.getOrNull()
        if (vivoDm != null) {
            for (m in vivoDm.declaredMethods.filter { it.name == "runShellWithResult" }) {
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CHARGING, true)) return@intercept chain.proceed()
                        val cmd = chain.args[0] as? String ?: return@intercept chain.proceed()
                        val lower = cmd.lowercase()
                        val isLimit = lower.contains("fex") || lower.contains("fix_temp") || lower.contains("bypass_charge") || lower.contains("current_max") || (lower.contains("echo") && lower.contains("tbat"))
                        if (isLimit) {
                            HookLogger.log(LogLevel.INFO, TAG, "VivoDm runShell blocked: $cmd")
                            return@intercept ""
                        }
                        chain.proceed()
                    }
                    count++
                }
            }
        }
        return count
    }

    private fun hookShellWrite(module: XposedModule, loader: ClassLoader): Int {
        val clazz = runCatching { Class.forName(CLASS_FUEL_H, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_FUEL_H not found: $it")
            return 0
        }
        var count = 0
        for (m in clazz.declaredMethods.filter { it.name == "r" && it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CHARGING, true)) return@intercept chain.proceed()
                    val cmd = chain.args[0] as? String ?: return@intercept chain.proceed()
                    val lower = cmd.lowercase()
                    val isLimit = lower.contains("fex_") || lower.contains("fix_temp") || lower.contains("current") || lower.contains("ibus") || lower.contains("fex")
                    if (isLimit) {
                        HookLogger.log(LogLevel.INFO, TAG, "h#r blocked limit cmd: $cmd")
                        return@intercept ""
                    }
                    chain.proceed()
                }
                HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#r(String) -> filter fex")
                count++
            }
        }
        for (m in clazz.declaredMethods.filter { it.name == "L" && it.parameterTypes.size == 2 }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CHARGING, true)) return@intercept chain.proceed()
                    val key = chain.args[0] as? String ?: ""
                    if (key.contains("fex") || key.contains("fix_temp") || key.contains("custom")) {
                        HookLogger.log(LogLevel.INFO, TAG, "h#L($key) blocked")
                        return@intercept null
                    }
                    chain.proceed()
                }
                count++
            }
        }
        return count
    }

    private companion object {
        const val TAG = "fuel"
        const val CLASS_BATTERY_UTILS = "r0.f"
        const val CLASS_FUEL_H = "com.vivo.fuelsummary.h"
    }
}
