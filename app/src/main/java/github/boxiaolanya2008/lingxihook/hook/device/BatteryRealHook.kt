package github.boxiaolanya2008.lingxihook.hook.device

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule

/**
 * 鐪熷疄鐢甸噺锛坅ndroid system_server锛夛細
 * BatteryService 娲惧彂 ACTION_BATTERY_CHANGED 鐨?level 缁?health 鏇茬嚎/鍏虫満鐢靛帇骞虫粦鍚庝笌 FG raw_soc 鍙樊 3~8%锛? * 棣栨牸鑰愮敤 30~60m 鍚庢毚璺屽嵆 UI 婊炲悗 raw 鐨勮〃鐜般€? * 姝ゅ鍦?system_server 涓嫤鎴箍鎾?extras level/scale/voltage锛屼娇 UI=raw 骞舵彁楂樺叧鏈虹數鍘嬭鏁板宸€? */
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
                        if (!HookConfig.isEnabled(SystemHook.FEATURE_REAL_BATTERY, true)) return@intercept chain.proceed()
                        val result = chain.proceed()
                        // 灏濊瘯鎶?intent extra level 鐢?raw 瑕嗗啓锛堣嫢 intent 鍦?args 涓級
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
        // 鍏滃簳锛氭嫤鎴墍鏈?sendBroadcast 鐨?Intent锛宭evel->raw
        runCatching {
            val ctxClazz = Class.forName("android.content.Context", false, loader)
            for (m in ctxClazz.declaredMethods.filter { it.name == "sendBroadcast" || it.name == "sendStickyBroadcast" }) {
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(SystemHook.FEATURE_REAL_BATTERY, true)) return@intercept chain.proceed()
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
                    if (!HookConfig.isEnabled(SystemHook.FEATURE_REAL_BATTERY, true)) return@intercept result
                    val thisFile = chain.thisObject as? java.io.FileReader ?: return@intercept result
                    val path = runCatching {
                        val f = java.io.FileReader::class.java.getDeclaredField("path")
                        f.isAccessible = true
                        f.get(thisFile) as? String
                    }.getOrNull() ?: return@intercept result
                    // 涓嶇洿鎺ユ敼 capacity 鏂囦欢锛岄伩鍏嶅唴鏍告贩涔憋紝浠呮棩蹇?                    result
                }
                count++
            }
        }
        // 鐩存帴鎷︽埅 BufferedReader readLine 瀵?capacity 鐨勮鍙栵紝鏇挎崲涓?raw
        val brClazz = runCatching { Class.forName("java.io.BufferedReader", false, loader) }.getOrNull() ?: return count
        for (m in brClazz.declaredMethods.filter { it.name == "readLine" && it.parameterTypes.isEmpty() }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    val result = chain.proceed() as? String ?: return@intercept chain.proceed()
                    if (!HookConfig.isEnabled(SystemHook.FEATURE_REAL_BATTERY, true)) return@intercept result
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
