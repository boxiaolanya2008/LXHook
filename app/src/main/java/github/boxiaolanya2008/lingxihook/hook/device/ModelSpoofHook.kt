package github.boxiaolanya2008.lingxihook.hook.device

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule

/**
 * 机型伪装：PD2520 -> PD2502，V2520A -> V2502A
 * 拦截 Build 与 SystemProperties 中所有型号获取，PD2520 替换为 PD2502、V2520A 替换为 V2502A，
 * 使相机 FeatureConfig 误判为 PD2502（vivo X300 Pro 同款平台）从而加载对应配置。
 */
class ModelSpoofHook {

    fun install(module: XposedModule, loader: ClassLoader) {
        var hooked = 0
        hooked += hookBuild()
        hooked += hookSystemProperties(module, loader)
        if (hooked > 0) HookLogger.log(LogLevel.INFO, TAG, "installed ($hooked hooks) PD2520 -> PD2502")
    }

    private fun hookBuild(): Int {
        return runCatching {
            val build = Class.forName("android.os.Build")
            fun spoofField(name: String) {
                runCatching {
                    val f = build.getDeclaredField(name)
                    f.isAccessible = true
                    val cur = f.get(null) as? String ?: return
                    var target: String? = null
                    if (cur.contains(SOURCE) || cur == SOURCE) {
                        target = cur.replace(SOURCE, TARGET)
                    } else if (cur.contains(SOURCE_V) || cur == SOURCE_V) {
                        target = cur.replace(SOURCE_V, TARGET_V)
                    }
                    if (target != null) {
                        try {
                            val mod = java.lang.reflect.Field::class.java.getDeclaredField("modifiers").apply { isAccessible = true }
                            mod.setInt(f, f.modifiers and java.lang.reflect.Modifier.FINAL.inv())
                        } catch (_: Exception) {}
                        f.set(null, target)
                        HookLogger.log(LogLevel.INFO, TAG, "Build.$name $cur -> $target")
                    }
                }
            }
            spoofField("MODEL")
            spoofField("PRODUCT")
            spoofField("DEVICE")
            spoofField("BOARD")
            1
        }.getOrDefault(0)
    }

    private fun hookSystemProperties(module: XposedModule, loader: ClassLoader): Int {
        var count = 0
        val targets = listOf(
            "android.os.SystemProperties",
            "com.android.camera.utils.SystemProperties"
        )
        for (clsName in targets) {
            val clazz = runCatching { Class.forName(clsName, false, loader) }.getOrNull() ?: continue
            for (m in clazz.declaredMethods.filter { it.name == "get" }) {
                val params = m.parameterTypes
                if (params.isNotEmpty() && params[0] == String::class.java) {
                    runCatching {
                        module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                            val key = chain.args.getOrNull(0) as? String
                            val result = chain.proceed() as? String
                            if (result != null) {
                                if (result.contains(SOURCE)) {
                                    val spoofed = result.replace(SOURCE, TARGET)
                                    HookLogger.log(LogLevel.INFO, TAG, "SystemProperties.get($key) $result -> $spoofed")
                                    return@intercept spoofed
                                }
                                if (result.contains(SOURCE_V)) {
                                    val spoofed = result.replace(SOURCE_V, TARGET_V)
                                    HookLogger.log(LogLevel.INFO, TAG, "SystemProperties.get($key) $result -> $spoofed")
                                    return@intercept spoofed
                                }
                            }
                            result ?: chain.proceed()
                        }
                        count++
                    }
                }
            }
            for (m in clazz.declaredMethods.filter { it.name == "get" || it.name == "getString" }) {
                if (m.parameterTypes.size == 2 && m.parameterTypes[0] == String::class.java && m.parameterTypes[1] == String::class.java) {
                    runCatching {
                        module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                            val key = chain.args.getOrNull(0) as? String
                            val def = chain.args.getOrNull(1) as? String
                            val result = chain.proceed() as? String ?: def
                            if (result != null) {
                                if (result.contains(SOURCE)) {
                                    val spoofed = result.replace(SOURCE, TARGET)
                                    HookLogger.log(LogLevel.INFO, TAG, "SystemProperties.get($key,$def) $result -> $spoofed")
                                    return@intercept spoofed
                                }
                                if (result.contains(SOURCE_V)) {
                                    val spoofed = result.replace(SOURCE_V, TARGET_V)
                                    HookLogger.log(LogLevel.INFO, TAG, "SystemProperties.get($key,$def) $result -> $spoofed")
                                    return@intercept spoofed
                                }
                            }
                            result ?: def
                        }
                        count++
                    }
                }
            }
        }
        return count
    }

    private fun isModelKey(key: String): Boolean {
        return key.contains("model", true) || key.contains("product", true) || key.contains("device", true) ||
            key == "ro.product.model.bbk" || key == "ro.product.model" || key == "ro.vivo.product.model" ||
            key == "ro.vivo.market.name" || key == "ro.product.name"
    }

    private companion object {
        const val TAG = "model"
        const val SOURCE = "PD2520"
        const val TARGET = "PD2502"
        const val SOURCE_V = "V2520A"
        const val TARGET_V = "V2502A"
    }
}
