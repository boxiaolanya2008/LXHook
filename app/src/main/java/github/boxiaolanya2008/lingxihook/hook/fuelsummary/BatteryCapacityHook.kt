package github.boxiaolanya2008.lingxihook.hook.fuelsummary

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * 电池容量锁最大（Hook 点来自 jadx 反编译 com.vivo.fuelsummary 2.5.0.3）。
 *
 * 容量链：
 * - com.vivo.fuelsummary.battery.health.a#b(Context):int 设计容量→健康曲线
 * - com.vivo.fuelsummary.battery.health.a#c(Context,int,int,int):int 健康度
 * - com.vivo.fuelsummary.battery.health.g#f():int soh 节点
 * - com.vivo.fuelsummary.battery.health.g#d():int ui_soh 4.0
 * - com.vivo.fuelsummary.h#u(f)/m(String)/C(String) 对 capacity_mah_from_battery_health / soh 节点
 *
 * 方案：已开启时健康度强制 100，容量节点返回最大设计值，关闭走原逻辑。
 */
class BatteryCapacityHook {

    fun install(module: XposedModule, param: PackageLoadedParam) {
        val loader = param.defaultClassLoader
        var hooked = 0
        hooked += hookHealthCurves(module, loader)
        hooked += hookCapacityNodes(module, loader)
        if (hooked == 0) {
            HookLogger.log(LogLevel.WARN, TAG, "no capacity methods hooked, maybe obfuscation changed")
        } else {
            HookLogger.log(LogLevel.INFO, TAG, "installed ($hooked hooks) capacity max")
        }
    }

    private fun hookHealthCurves(module: XposedModule, loader: ClassLoader): Int {
        var count = 0
        val healthA = runCatching { Class.forName(CLASS_HEALTH_A, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_HEALTH_A not found: $it")
            return 0
        }
        for (m in healthA.declaredMethods.filter { it.name == "b" && it.parameterTypes.size == 1 }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CAPACITY, true)) return@intercept chain.proceed()
                    val origin = chain.proceed() as? Int
                    HookLogger.log(LogLevel.INFO, TAG, "${healthA.simpleName}#b: $origin -> 100 (capacity max)", persist = false)
                    100
                }
                HookLogger.log(LogLevel.INFO, TAG, "hooked ${healthA.name}#b -> 100")
                count++
            }
        }
        for (m in healthA.declaredMethods.filter { it.name == "c" && it.parameterTypes.size == 4 }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CAPACITY, true)) return@intercept chain.proceed()
                    100
                }
                HookLogger.log(LogLevel.INFO, TAG, "hooked ${healthA.name}#c -> 100")
                count++
            }
        }
        for (m in healthA.declaredMethods.filter { it.name == "a" && it.parameterTypes.size == 4 }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CAPACITY, true)) return@intercept chain.proceed()
                    100
                }
                count++
            }
        }
        for (m in healthA.declaredMethods.filter { it.name == "f" && it.parameterTypes.isEmpty() }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CAPACITY, true)) return@intercept chain.proceed()
                    100
                }
                count++
            }
        }
        val healthG = runCatching { Class.forName(CLASS_HEALTH_G, false, loader) }.getOrNull()
        healthG?.let { clazz ->
            runCatching { clazz.getDeclaredMethod("f") }.onSuccess { m ->
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CAPACITY, true)) return@intercept chain.proceed()
                        100
                    }
                    HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#f -> 100 (soh)")
                    count++
                }
            }
            runCatching { clazz.getDeclaredMethod("d") }.onSuccess { m ->
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CAPACITY, true)) return@intercept chain.proceed()
                        100
                    }
                    count++
                }
            }
        }
        return count
    }

    private fun hookCapacityNodes(module: XposedModule, loader: ClassLoader): Int {
        val fuelH = runCatching { Class.forName(CLASS_FUEL_H, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_FUEL_H not found: $it")
            return 0
        }
        var count = 0
        for (m in fuelH.declaredMethods.filter { it.name == "u" && it.parameterTypes.size == 1 }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CAPACITY, true)) return@intercept chain.proceed()
                    val fObj = chain.args[0]
                    val result = chain.proceed() as? String
                    if (result == null) return@intercept result
                    val key = runCatching {
                        val bMethod = fObj.javaClass.getMethod("b", StringBuffer::class.java)
                        val sb = StringBuffer()
                        bMethod.invoke(fObj, sb) as? String
                    }.getOrNull() ?: fObj.toString()
                    val lower = key.lowercase()
                    val isCapacity = lower.contains("capacity") || lower.contains("soh") || lower.contains("mah") || lower.contains("health")
                    if (isCapacity) {
                        HookLogger.log(LogLevel.INFO, TAG, "h#u($key): $result -> 100 (capacity max)", persist = false)
                        return@intercept "100"
                    }
                    if (result.toIntOrNull() != null && result.toInt() < 100) {
                        val caller = Thread.currentThread().stackTrace.firstOrNull { it.className.contains("health", true) }?.className
                        if (caller != null) return@intercept "100"
                    }
                    result
                }
                count++
            }
        }
        for (m in fuelH.declaredMethods.filter { it.name == "m" && it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CAPACITY, true)) return@intercept chain.proceed()
                    val key = chain.args[0] as? String ?: return@intercept chain.proceed()
                    val lower = key.lowercase()
                    val isCapacity = lower.contains("capacity") || lower.contains("soh") || lower.contains("health") || lower.contains("ui_soh")
                    if (isCapacity) return@intercept "100"
                    chain.proceed()
                }
                count++
            }
        }
        for (m in fuelH.declaredMethods.filter { it.name == "C" && it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CAPACITY, true)) return@intercept chain.proceed()
                    val path = chain.args[0] as? String ?: return@intercept chain.proceed()
                    val lower = path.lowercase()
                    if (lower.contains("capacity") || lower.contains("soh") || lower.contains("health")) return@intercept 100
                    chain.proceed()
                }
                count++
            }
        }
        count += hookRealSoc(module, loader)
        return count
    }

    private fun hookRealSoc(module: XposedModule, loader: ClassLoader): Int {
        var count = 0
        val fuelService = runCatching { Class.forName(CLASS_FUEL_SERVICE, false, loader) }.getOrNull() ?: return 0
        // 最真：FG true_soc 含 OCV 修正，比 raw 更准；同时禁三处平滑上报
        val trueField = runCatching { fuelService.getDeclaredField("f2180g1").apply { isAccessible = true } }.getOrNull()
        val filteredField = runCatching { fuelService.getDeclaredField("f2177f1").apply { isAccessible = true } }.getOrNull()
        for (m in fuelService.declaredMethods.filter { it.name == "z0" && it.parameterTypes.size == 1 }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    val result = chain.proceed()
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CAPACITY, true) && !HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CYCLE, true)) return@intercept result
                    runCatching {
                        val svc = chain.thisObject
                        val fLevel = fuelService.getDeclaredField("f2145q").apply { isAccessible = true }
                        // 优先 true，其次 raw
                        val trueVal = trueField?.getInt(svc) ?: -1
                        val rawVal = fuelService.getDeclaredField("f2149s").apply { isAccessible = true }.getInt(svc)
                        val target = if (trueVal in 1..100) trueVal else rawVal
                        val ui = fLevel.getInt(svc)
                        if (target in 1..100 && ui != target) {
                            fLevel.setInt(svc, target)
                            HookLogger.log(LogLevel.INFO, TAG, "real soc ui $ui -> ${if (trueVal in 1..100) "true" else "raw"} $target", persist = false)
                        }
                    }
                    result
                }
                HookLogger.log(LogLevel.INFO, TAG, "hooked FuelSummaryService#z0 -> ui=true/raw")
                count++
            }
        }
        // 禁平滑：FG_EX_UI_JUMP / LEVEL_VBAT_MISMATCH / QUICK_DROP 上报虽不改 UI，但其所在 l/O 方法会滞后 UI，整体阻断更均匀
        val innerNames = listOf("com.vivo.fuelsummary.FuelSummaryService\$e", "com.vivo.fuelsummary.FuelSummaryService\$d", "com.vivo.fuelsummary.FuelSummaryService\$d\$e", "com.vivo.fuelsummary.FuelSummaryService\$d\$g")
        for (inner in innerNames) {
            val clazz = runCatching { Class.forName(inner, false, loader) }.getOrNull() ?: continue
            for (m in clazz.declaredMethods.filter { it.name == "l" || it.name == "O" || it.name == "H" }) {
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CAPACITY, true)) return@intercept chain.proceed()
                        // 对 l() 的 FG_EX_UI_JUMP 阈 >1 判定，仍让其执行但后续 ui 已覆写为 true，故此处放行不阻断上报，仅防其内部对 f2145q 的二次滞后
                        chain.proceed()
                    }
                    count++
                }
            }
        }
        // FCC 平滑：首格耐用主因是 FG 学偏的 FCC 虚高，虽已锁 SOH 100，仍需让 FG 的 filtered 贴 true
        for (m in fuelService.declaredMethods.filter { it.name == "onCreate" }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    val result = chain.proceed()
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CAPACITY, true)) return@intercept result
                    runCatching {
                        val svc = chain.thisObject
                        val fLevel = fuelService.getDeclaredField("f2145q").apply { isAccessible = true }
                        val tVal = trueField?.getInt(svc) ?: -1
                        val rVal = fuelService.getDeclaredField("f2149s").apply { isAccessible = true }.getInt(svc)
                        val target = if (tVal in 1..100) tVal else rVal
                        if (target in 1..100) fLevel.setInt(svc, target)
                    }
                    result
                }
                count++
            }
        }
        return count
    }

    private companion object {
        const val TAG = "fuel"
        const val CLASS_HEALTH_A = "com.vivo.fuelsummary.battery.health.a"
        const val CLASS_HEALTH_G = "com.vivo.fuelsummary.battery.health.g"
        const val CLASS_FUEL_H = "com.vivo.fuelsummary.h"
        const val CLASS_FUEL_SERVICE = "com.vivo.fuelsummary.FuelSummaryService"
    }
}
