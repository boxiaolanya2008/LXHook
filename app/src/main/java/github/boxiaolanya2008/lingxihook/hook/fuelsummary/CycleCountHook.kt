package github.boxiaolanya2008.lingxihook.hook.fuelsummary

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * 循环次数锁 5 次（Hook 点来自 jadx 反编译 com.vivo.fuelsummary 2.5.0.3）。
 *
 * 循环链：
 * - com.vivo.fuelsummary.battery.health.g#b() 读 /sys/class/fuelsummary/cycle
 * - com.vivo.fuelsummary.h#z(String)/C(String) 读文件
 * - com.vivo.fuelsummary.h#u(f) 读节点
 * - SharedPreferences "cycle"
 *
 * 方案：已开启时全部返回 5，关闭走原逻辑。
 */
class CycleCountHook {

    fun install(module: XposedModule, param: PackageLoadedParam) {
        val loader = param.defaultClassLoader
        var hooked = 0
        hooked += hookHealthGCycle(module, loader)
        hooked += hookFuelHNodes(module, loader)
        if (hooked == 0) {
            HookLogger.log(LogLevel.WARN, TAG, "no cycle methods hooked, maybe obfuscation changed")
        } else {
            HookLogger.log(LogLevel.INFO, TAG, "installed ($hooked hooks) cycle=5")
        }
    }

    private fun hookHealthGCycle(module: XposedModule, loader: ClassLoader): Int {
        val clazz = runCatching { Class.forName(CLASS_HEALTH_G, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_HEALTH_G not found: $it")
            return 0
        }
        var count = 0
        runCatching { clazz.getDeclaredMethod("b") }.onSuccess { m ->
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CYCLE, true)) return@intercept chain.proceed()
                    val origin = chain.proceed() as? Int
                    if (origin != TARGET_CYCLE) HookLogger.log(LogLevel.INFO, TAG, "${clazz.simpleName}#b: $origin -> $TARGET_CYCLE")
                    TARGET_CYCLE
                }
                HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#b -> $TARGET_CYCLE")
                count++
            }
        }
        return count
    }

    private fun hookFuelHNodes(module: XposedModule, loader: ClassLoader): Int {
        val clazz = runCatching { Class.forName(CLASS_FUEL_H, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_FUEL_H not found: $it")
            return 0
        }
        var count = 0
        for (m in clazz.declaredMethods.filter { it.name == "C" && it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CYCLE, true)) return@intercept chain.proceed()
                    val path = chain.args[0] as? String ?: return@intercept chain.proceed()
                    if (path.lowercase().contains("cycle")) {
                        HookLogger.log(LogLevel.INFO, TAG, "h#C($path): -> $TARGET_CYCLE", persist = false)
                        return@intercept TARGET_CYCLE
                    }
                    chain.proceed()
                }
                count++
            }
        }
        for (m in clazz.declaredMethods.filter { it.name == "z" && it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CYCLE, true)) return@intercept chain.proceed()
                    val path = chain.args[0] as? String ?: return@intercept chain.proceed()
                    if (path.lowercase().contains("cycle")) return@intercept TARGET_CYCLE.toString()
                    chain.proceed()
                }
                count++
            }
        }
        for (m in clazz.declaredMethods.filter { it.name == "u" && it.parameterTypes.size == 1 }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CYCLE, true)) return@intercept chain.proceed()
                    val fObj = chain.args[0] ?: return@intercept chain.proceed()
                    val key = runCatching {
                        val bMethod = fObj.javaClass.getMethod("b", StringBuffer::class.java)
                        val sb = StringBuffer()
                        bMethod.invoke(fObj, sb) as? String ?: fObj.toString()
                    }.getOrNull() ?: fObj.toString()
                    if (key.lowercase().contains("cycle")) {
                        HookLogger.log(LogLevel.INFO, TAG, "h#u($key) -> $TARGET_CYCLE", persist = false)
                        return@intercept TARGET_CYCLE.toString()
                    }
                    chain.proceed()
                }
                count++
            }
        }
        for (m in clazz.declaredMethods.filter { it.name == "m" && it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoFuelSummaryHook.FEATURE_CYCLE, true)) return@intercept chain.proceed()
                    val key = chain.args[0] as? String ?: return@intercept chain.proceed()
                    if (key.lowercase().contains("cycle") || key == "cycle") return@intercept TARGET_CYCLE.toString()
                    chain.proceed()
                }
                count++
            }
        }
        return count
    }

    private companion object {
        const val TAG = "fuel"
        const val CLASS_HEALTH_G = "com.vivo.fuelsummary.battery.health.g"
        const val CLASS_FUEL_H = "com.vivo.fuelsummary.h"
        const val TARGET_CYCLE = 5
    }
}
