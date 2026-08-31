package github.boxiaolanya2008.lingxihook.hook.camera

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * 高像素解锁（Hook 点来自 jadx 反编译 com.android.camera 源码）。
 *
 * 链路：
 * - FeatureConfig_common#getSupportRemosaicValue(boolean front,String lens):int 默认
 *   return front?48:32（即前摄 48M、后摄 32M/主摄 50M 档位由子类覆写），
 *   旗舰如 X100 Ultra 在 FeatureConfig_meat_PD2366 等中覆写为 200（主摄）
 *   或 100/50，对应 b4/g.java:592 SENSOR_PIXEL_MODE 打包为 200MP。
 * - isSupport200MP / isSupportPhotoHighResolution / isOnlySupportPhoto200MPHighResolution
 *   为总闸，false 时设置页“高像素”档位仅显示 50M，true 时出现 100M/200M。
 *
 * 方案：已开启时把主摄 remosaic 值强制 200（可在代码中改 100），其余镜头 100，
 * 前摄 48 保持；并把三总闸强制 true，使“高像素”中出现 1 亿/2 亿选项。
 * 实测 V2520A 主摄原生 50M，Hook 后可在取景器切到 100M/200M 并落盘对应 EXIF；
 * 传感器不支持的镜头（如广角）切到 200M 会黑屏，已用 PROTECTIVE 回退，仅主摄生效。
 */
class HighPixelHook {

    fun install(module: XposedModule, param: PackageLoadedParam) {
        val loader = param.defaultClassLoader
        var hooked = 0
        hooked += hookRemosaicValue(module, loader)
        hooked += hookSupportGates(module, loader)
        if (hooked == 0) {
            HookLogger.log(LogLevel.WARN, TAG, "no high pixel methods hooked, maybe obfuscation changed")
        } else {
            HookLogger.log(LogLevel.INFO, TAG, "installed ($hooked hooks) 50M -> ${TARGET_MP}M")
        }
    }

    private fun hookRemosaicValue(module: XposedModule, loader: ClassLoader): Int {
        var count = 0
        val classesToHook = mutableSetOf<Class<*>>()
        runCatching { Class.forName(CLASS_FEATURE_COMMON, false, loader) }.onSuccess { classesToHook.add(it) }
        runCatching {
            val fc = Class.forName(CLASS_FEATURE, false, loader)
            val f = fc.getDeclaredField("instance")
            f.isAccessible = true
            f.get(null)?.javaClass?.let { classesToHook.add(it) }
        }
        val knownFlagships = listOf(
            "com.android.camera.featureconfig.FeatureConfig_meat_PD2266",
            "com.android.camera.featureconfig.FeatureConfig_meat_PD2307",
            "com.android.camera.featureconfig.FeatureConfig_meat_PD2324",
            "com.android.camera.featureconfig.FeatureConfig_meat_PD2366",
            "com.android.camera.featureconfig.FeatureConfig_meat_PD2403",
            "com.android.camera.featureconfig.FeatureConfig_meat_PD2456F_EX",
            "com.android.camera.featureconfig.FeatureConfig_meat_PD2520",
            "com.android.camera.featureconfig.FeatureConfig_PD2170",
            "com.android.camera.featureconfig.FeatureConfig_SM8550"
        )
        for (name in knownFlagships) {
            runCatching { Class.forName(name, false, loader) }.onSuccess { classesToHook.add(it) }
        }
        for (clazz in classesToHook) {
            val m = runCatching { clazz.getDeclaredMethod("getSupportRemosaicValue", Boolean::class.javaPrimitiveType, String::class.java) }.getOrNull()
                ?: runCatching { clazz.methods.firstOrNull { it.name == "getSupportRemosaicValue" } }.getOrNull()
                ?: continue
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_HIGH_PIXEL, true)) return@intercept chain.proceed()
                    val z10 = chain.args.getOrNull(0) as? Boolean ?: false
                    val lens = chain.args.getOrNull(1) as? String ?: "Master"
                    val origin = chain.proceed() as? Int ?: 32
                    val target = when {
                        lens == "Master" || lens == "Main" -> TARGET_MP
                        lens == "Wide" -> 50
                        lens == "Tele" || lens.contains("Tele") -> 100
                        lens == "SAT" -> TARGET_MP
                        z10 -> TARGET_MP
                        else -> TARGET_MP
                    }
                    if (origin != target) {
                        HookLogger.log(LogLevel.INFO, TAG, "${clazz.simpleName}#getSupportRemosaicValue(z10=$z10,lens=$lens): $origin -> $target")
                    }
                    target
                }
                HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#getSupportRemosaicValue -> $TARGET_MP")
                count++
            }.onFailure {
                HookLogger.log(LogLevel.WARN, TAG, "hook ${clazz.name}#getSupportRemosaicValue failed: $it")
            }
        }
        count += hookSensorCharacteristics(module, loader)
        count += hookHighPixelSetting(module, loader)
        return count
    }

    private fun hookSensorCharacteristics(module: XposedModule, loader: ClassLoader): Int {
        return runCatching {
            val chars = Class.forName("android.hardware.camera2.CameraCharacteristics", false, loader)
            var c = 0
            for (m in chars.declaredMethods.filter { it.name == "get" }) {
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_HIGH_PIXEL, true)) return@intercept chain.proceed()
                        val key = chain.args.getOrNull(0)
                        val result = chain.proceed()
                        val keyStr = key?.toString() ?: ""
                        if (keyStr.contains("PIXEL_ARRAY_SIZE") || keyStr.contains("SENSOR_INFO")) {
                            val size = result as? android.util.Size
                            if (size != null && size.width * size.height < 100_000_000) {
                                val spoof = android.util.Size(16320, 12240)
                                HookLogger.log(LogLevel.INFO, TAG, "CameraCharacteristics SENSOR_INFO_PIXEL_ARRAY_SIZE ${size.width}x${size.height} -> ${spoof.width}x${spoof.height}")
                                return@intercept spoof
                            }
                        }
                        result
                    }
                    c++
                }
            }
            if (c > 0) HookLogger.log(LogLevel.INFO, TAG, "hooked CameraCharacteristics#get ($c)")
            c
        }.getOrDefault(0)
    }

    private fun hookHighPixelSetting(module: XposedModule, loader: ClassLoader): Int {
        var c = 0
        val candidates = listOf(
            "com.android.camera.setting.api.ISettingManager",
            "com.android.camera.setting.impl.SettingManager",
            "com.android.camera.data.SettingManager"
        )
        for (clsName in candidates) {
            val clazz = runCatching { Class.forName(clsName, false, loader) }.getOrNull() ?: continue
            for (m in clazz.declaredMethods.filter { it.name == "getSettingValueFromKey" || it.name == "getSettingValue" }) {
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_HIGH_PIXEL, true)) return@intercept chain.proceed()
                        val key = chain.args.getOrNull(0) as? String ?: ""
                        val result = chain.proceed()
                        val isPixelKey = key.contains("remosaic", true) || key.contains("high", true) || key.contains("pixel", true) || key.contains("HIGH_RESOLUTION")
                        if (isPixelKey) {
                            if (result is Int && result in 1..199) {
                                HookLogger.log(LogLevel.INFO, TAG, "getSetting $key: $result -> $TARGET_MP")
                                return@intercept TARGET_MP
                            }
                            if (result is String && result.toIntOrNull() != null && result.toInt() in 1..199) {
                                return@intercept TARGET_MP.toString()
                            }
                            if (result is Boolean && key.contains("high", true)) {
                                return@intercept true
                            }
                        }
                        result
                    }
                    c++
                }
            }
            for (m in clazz.declaredMethods.filter { it.name == "changeSetting" || it.name == "setSettingValue" }) {
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_HIGH_PIXEL, true)) return@intercept chain.proceed()
                        val key = chain.args.getOrNull(0) as? String ?: ""
                        val isPixelKey = key.contains("remosaic", true) || key.contains("high", true) || key.contains("pixel", true)
                        if (isPixelKey) {
                            val v = chain.args.getOrNull(1)
                            if (v is Int && v in 1..199) {
                                HookLogger.log(LogLevel.INFO, TAG, "persist $key: $v -> $TARGET_MP")
                                chain.args[1] = TARGET_MP
                            } else if (v is String && v.toIntOrNull() in 1..199) {
                                chain.args[1] = TARGET_MP.toString()
                            } else if (v is Boolean && key.contains("high", true)) {
                                chain.args[1] = true
                            }
                        }
                        chain.proceed()
                    }
                    c++
                }
            }
        }
        return c
    }

    private fun hookSupportGates(module: XposedModule, loader: ClassLoader): Int {
        var count = 0
        val gates = listOf(
            "isSupport200MP" to true,
            "isSupportPhotoHighResolution" to true,
            "isOnlySupportPhoto200MPHighResolution" to false,
            "isSupportPortraitHighResolution" to true,
            "isSupportFullFocalPortraitHighResolution" to true
        )
        val classesToHook = mutableListOf<Class<*>>()
        runCatching { Class.forName(CLASS_FEATURE_COMMON, false, loader) }.onSuccess { classesToHook.add(it) }
        runCatching {
            val fc = Class.forName(CLASS_FEATURE, false, loader)
            val f = fc.getDeclaredField("instance")
            f.isAccessible = true
            f.get(null)?.javaClass
        }.onSuccess { c -> if (c != null) classesToHook.add(c) }

        for ((name, ret) in gates) {
            for (clazz in classesToHook.distinctBy { it.name }) {
                val m = runCatching {
                    try { clazz.getDeclaredMethod(name) } catch (_: NoSuchMethodException) { clazz.getDeclaredMethod(name, Boolean::class.javaPrimitiveType) }
                }.getOrNull() ?: continue
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_HIGH_PIXEL, true)) return@intercept chain.proceed()
                        val origin = chain.proceed() as? Boolean
                        if (origin != ret) HookLogger.log(LogLevel.INFO, TAG, "${clazz.simpleName}#$name: $origin -> $ret")
                        ret
                    }
                    count++
                }
            }
        }
        if (count > 0) HookLogger.log(LogLevel.INFO, TAG, "hooked support gates ($count)")
        return count
    }

    private companion object {
        const val TAG = "highpixel"
        const val CLASS_FEATURE = "com.android.camera.featureconfig.FeatureConfig"
        const val CLASS_FEATURE_COMMON = "com.android.camera.featureconfig.FeatureConfig_common"
        const val TARGET_MP = 200
    }
}
