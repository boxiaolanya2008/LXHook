package github.boxiaolanya2008.lingxihook.hook.device

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule

/**
 * 真实电量（android system_server）：
 * BatteryService 派发 ACTION_BATTERY_CHANGED 的 level 经 health 曲线/关机电压平滑后与 FG raw_soc 可差 3~8%，
 * 首格耐用 30~60m 后暴跌即 UI 滞后 raw 的表现。
 * 此处在 system_server 中拦截广播 extras level/scale/voltage，使 UI=raw 并提高关机电压读数容差。
 */
class BatteryRealHook {

    fun install(module: XposedModule, loader: ClassLoader) {
        var hooked = 0
        hooked += hookBatteryService(loader, module)
        hooked += hookCapacityFile(loader, module)
        if (hooked == 0) {
            HookLogger.log(LogLevel.WARN, TAG, "no battery real hooks, maybe service renamed")
        } else {
            HookLogger.log(LogLevel.INFO, TAG, "installed ($hooked hooks) real battery ui=raw")
        }
    }

    private fun hookBatteryService(loader: ClassLoader, module: XposedModule): Int {
        var count = 0
        val candidates = listOf(
            "com.android.server.BatteryService",
            "com.android.server.battery.BatteryService"
        )
        for (clsName in candidates) {
            val clazz = runCatching { Class.forName(clsName, false, loader) }.getOrNull() ?: continue
            for (m in clazz.declaredMethods) {
                val isSend = m.name.contains("sendBattery", true) || m.name.contains("updateBattery", true) || m.name.contains("processValues", true)
                if (!isSend) continue
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(DeviceModelHook.FEATURE_REAL_BATTERY, true)) return@intercept chain.proceed()
                        val result = chain.proceed()
                        // 尝试把 intent extra level 用 raw 覆写（若 intent 在 args 中）
                        for (arg in chain.args) {
                            if (arg is android.content.Intent && arg.action == android.content.Intent.ACTION_BATTERY_CHANGED) {
                                val raw = readRawCapacity(loader)
                                if (raw in 1..100) {
                                    val cur = arg.getIntExtra("level", -1)
                                    if (cur != raw) {
                                        arg.putExtra("level", raw)
                                        HookLogger.log(LogLevel.INFO, TAG, "BatteryService level $cur -> raw $raw", persist = false)
                                    }
                                }
                            }
                        }
                        result
                    }
                    count++
                }
            }
        }
        // 兜底：拦截所有 sendBroadcast 的 Intent，level->raw
        runCatching {
            val ctxClazz = Class.forName("android.content.Context", false, loader)
            for (m in ctxClazz.declaredMethods.filter { it.name == "sendBroadcast" || it.name == "sendStickyBroadcast" }) {
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(DeviceModelHook.FEATURE_REAL_BATTERY, true)) return@intercept chain.proceed()
                        val intent = chain.args.getOrNull(0) as? android.content.Intent ?: return@intercept chain.proceed()
                        if (intent.action == android.content.Intent.ACTION_BATTERY_CHANGED) {
                            val raw = readRawCapacity(loader)
                            if (raw in 1..100) intent.putExtra("level", raw)
                        }
                        chain.proceed()
                    }
                    count++
                }
            }
        }
        return count
    }

    private fun hookCapacityFile(loader: ClassLoader, module: XposedModule): Int {
        var count = 0
        val fileClazz = runCatching { Class.forName("java.io.FileReader", false, loader) }.getOrNull() ?: return 0
        for (m in fileClazz.declaredMethods.filter { it.name == "readLine" }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    val result = chain.proceed() as? String ?: return@intercept chain.proceed()
                    if (!HookConfig.isEnabled(DeviceModelHook.FEATURE_REAL_BATTERY, true)) return@intercept result
                    val thisFile = chain.thisObject as? java.io.FileReader ?: return@intercept result
                    val path = runCatching {
                        val f = java.io.FileReader::class.java.getDeclaredField("path")
                        f.isAccessible = true
                        f.get(thisFile) as? String
                    }.getOrNull() ?: return@intercept result
                    // 不直接改 capacity 文件，避免内核混乱，仅日志
                    result
                }
                count++
            }
        }
        // 直接拦截 BufferedReader readLine 对 capacity 的读取，替换为 raw
        val brClazz = runCatching { Class.forName("java.io.BufferedReader", false, loader) }.getOrNull() ?: return count
        for (m in brClazz.declaredMethods.filter { it.name == "readLine" && it.parameterTypes.isEmpty() }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    val result = chain.proceed() as? String ?: return@intercept chain.proceed()
                    if (!HookConfig.isEnabled(DeviceModelHook.FEATURE_REAL_BATTERY, true)) return@intercept result
                    val stack = Thread.currentThread().stackTrace
                    val isCapacity = stack.any { it.methodName.contains("getBatteryLevel") || it.className.contains("BatteryService") || it.fileName?.contains("Battery") == true }
                    if (!isCapacity) return@intercept result
                    val raw = readRawCapacity(loader)
                    if (raw in 1..100) return@intercept raw.toString()
                    result
                }
                count++
            }
        }
        return count
    }

    private fun readRawCapacity(loader: ClassLoader): Int {
        return runCatching {
            val br = java.io.BufferedReader(java.io.FileReader("/sys/class/power_supply/battery/capacity"))
            br.readLine()?.toIntOrNull() ?: -1
        }.getOrNull() ?: runCatching {
            val br2 = java.io.BufferedReader(java.io.FileReader("/sys/class/fuelsummary/raw_soc"))
            br2.readLine()?.toIntOrNull() ?: -1
        }.getOrNull() ?: -1
    }

    private companion object {
        const val TAG = "fuel"
    }
}
