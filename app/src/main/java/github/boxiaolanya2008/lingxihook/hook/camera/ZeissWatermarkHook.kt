package github.boxiaolanya2008.lingxihook.hook.camera

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * ZEISS 水印解锁（Hook 点来自 jadx 反编译 com.android.camera 源码，非猜测）。
 *
 * 判定链：
 * - com.android.camera.featureconfig.FeatureConfig_common#isCameraSignedByZeiss() 默认 false
 *   高端 vivo（X 系列）机型在 FeatureConfig_meat_PD2266 / FeatureConfig_PD2170 等中覆写为 true，
 *   低端 iQOO（PD2520）保持 false。水印模板生成 gk/j.java、kj/y.java、kj/u0.java 等
 *   均以此值为总开关决定是否展示 “ZEISS” 边框/大师水印与蓝厂联合 logo。
 * - isSupportWatermarkZEISS() 内部直接 return isCameraSignedByZeiss()
 * - isSupportWatermarkBorder() 决定是否支持边框水印，旗舰 true，common false
 * - isSupportZeissColor(boolean) 决定是否支持蔡司自然色彩，旗舰 true
 * - supportWatermarkTmpl() 返回可用模板数组，PD2520 仅 2 项（DEFAULT_PHOTO/VIDEO），
 *   PD2266 等旗舰 8 项（DEFAULT_PHOTO/BORDER/SIGNATURE/CUSTOM/FEATURE/MASTER/CHINOISERIES/DEFAULT_VIDEO）
 *
 * 方案：已开启时把上述方法强制返回旗舰值，使 iQOO 在设置中出现 ZEISS 边框与大师水印，
 * 并让已生成的照片写入 ZEISS 联名标记。关闭开关则走原逻辑。
 *
 * 注意：isCameraSignedByZeiss / isSupportWatermarkZEISS 等为混淆后稳定的 FeatureConfig 方法名，
 * 随相机版本可能新增覆写类；本 Hook 同时拦截 FeatureConfig_common 与当前设备实际生效的
 * FeatureConfig_* 运行时类，避免仅改父类被子类覆写绕过。
 */
class ZeissWatermarkHook {

    fun install(module: XposedModule, param: PackageLoadedParam) {
        val loader = param.defaultClassLoader
        var hooked = 0
        hooked += hookFeatureConfigFamily(module, loader)
        hooked += hookStandardSizeConfig(module, loader)
        hooked += hookDeviceUtil(module, loader)
        hooked += hookSystemProperties(module, loader)
        hooked += hookBuildModel()
        hooked += hookFeatureConfigModel(module, loader)
        hooked += hookWatermarkUtils(module, loader)
        if (hooked == 0) {
            HookLogger.log(LogLevel.WARN, TAG, "no zeiss methods hooked, maybe obfuscation changed")
        } else {
            HookLogger.log(LogLevel.INFO, TAG, "installed ($hooked hooks)")
        }
    }

    private fun hookFeatureConfigFamily(module: XposedModule, loader: ClassLoader): Int {
        var count = 0
        val classesToHook = mutableListOf<Class<*>>()

        runCatching { Class.forName(CLASS_FEATURE_COMMON, false, loader) }
            .onSuccess { classesToHook.add(it) }
            .onFailure { HookLogger.log(LogLevel.WARN, TAG, "$CLASS_FEATURE_COMMON not found: $it") }

        runCatching {
            val fc = Class.forName(CLASS_FEATURE, false, loader)
            val field = fc.getDeclaredField("instance")
            field.isAccessible = true
            val inst = field.get(null)
            inst?.javaClass
        }.onSuccess { clazz ->
            if (clazz != null && classesToHook.none { it.name == clazz.name }) {
                classesToHook.add(clazz)
                HookLogger.log(LogLevel.INFO, TAG, "runtime FeatureConfig class: ${clazz.name}")
            }
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "get FeatureConfig.instance failed: $it")
        }

        for (clazz in classesToHook.distinctBy { it.name }) {
            count += hookBooleanTrue(module, clazz, "isCameraSignedByZeiss")
            count += hookBooleanTrue(module, clazz, "isSupportWatermarkZEISS")
            count += hookBooleanTrue(module, clazz, "isSupportWatermarkBorder")
            count += hookZeissColor(module, clazz)
            count += hookSupportTmpl(module, clazz)
            count += hookWatermarkVersion(module, clazz, loader)
        }
        return count
    }

    private fun hookBooleanTrue(module: XposedModule, clazz: Class<*>, name: String): Int {
        val method = runCatching { clazz.getDeclaredMethod(name) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "${clazz.name}#$name not found: $it")
            return 0
        }
        return runCatching {
            module.hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ZEISS, true)) return@intercept chain.proceed()
                    val origin = chain.proceed() as? Boolean
                    if (origin != true) {
                        HookLogger.log(LogLevel.INFO, TAG, "${clazz.simpleName}#$name: $origin -> true（ZEISS 已放行）")
                    }
                    true
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#$name -> true")
            1
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook ${clazz.name}#$name failed: $it")
        }.getOrDefault(0)
    }

    private fun hookZeissColor(module: XposedModule, clazz: Class<*>): Int {
        val method = runCatching {
            clazz.getDeclaredMethod("isSupportZeissColor", Boolean::class.javaPrimitiveType)
        }.getOrElse {
            runCatching { clazz.getDeclaredMethod("isSupportZeissColor", java.lang.Boolean.TYPE) }.getOrElse {
                runCatching { clazz.methods.firstOrNull { it.name == "isSupportZeissColor" } }.getOrNull()
                    ?: run {
                        HookLogger.log(LogLevel.WARN, TAG, "${clazz.name}#isSupportZeissColor not found: $it")
                        return 0
                    }
            }
        }
        return runCatching {
            module.hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ZEISS, true)) return@intercept chain.proceed()
                    true
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#isSupportZeissColor -> true")
            1
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook ${clazz.name}#isSupportZeissColor failed: $it")
        }.getOrDefault(0)
    }

    private fun hookSupportTmpl(module: XposedModule, clazz: Class<*>): Int {
        val method = runCatching { clazz.getDeclaredMethod("supportWatermarkTmpl") }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "${clazz.name}#supportWatermarkTmpl not found: $it")
            return 0
        }
        return runCatching {
            module.hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ZEISS, true)) return@intercept chain.proceed()
                    val origin = chain.proceed() as? Array<*>
                    val flagship = arrayOf(
                        "DEFAULT_PHOTO",
                        "BORDER_PHOTO",
                        "SIGNATURE_PHOTO",
                        "CUSTOM_PIC",
                        "FEATURE_PHOTO",
                        "MASTER_PHOTO",
                        "CHINOISERIES_PHOTO",
                        "DEFAULT_VIDEO"
                    )
                    if (origin == null || origin.size < flagship.size) {
                        HookLogger.log(LogLevel.INFO, TAG, "${clazz.simpleName}#supportWatermarkTmpl: ${origin?.size ?: 0} -> ${flagship.size}（旗舰模板已放行）")
                    }
                    flagship
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#supportWatermarkTmpl -> flagship 8")
            1
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook ${clazz.name}#supportWatermarkTmpl failed: $it")
        }.getOrDefault(0)
    }

    private fun hookWatermarkVersion(module: XposedModule, clazz: Class<*>, loader: ClassLoader): Int {
        val method = runCatching { clazz.getDeclaredMethod("getWatermarkVersion") }.getOrElse {
            return 0
        }
        return runCatching {
            module.hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ZEISS, true)) return@intercept chain.proceed()
                    val origin = chain.proceed()
                    val v4 = runCatching {
                        val wmVersion = Class.forName("com.android.camera.featureconfig.configuration.watermark.WMVersion", false, loader)
                        java.lang.Enum.valueOf(wmVersion as Class<out Enum<*>>, "V4")
                    }.getOrNull() ?: origin
                    if (origin != v4) {
                        HookLogger.log(LogLevel.INFO, TAG, "${clazz.simpleName}#getWatermarkVersion: $origin -> V4")
                    }
                    v4
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#getWatermarkVersion -> V4")
            1
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook ${clazz.name}#getWatermarkVersion failed: $it")
        }.getOrDefault(0)
    }

    private fun hookStandardSizeConfig(module: XposedModule, loader: ClassLoader): Int {
        var count = 0
        val targets = listOf(
            "com.android.camera.utils.watermark.size.StandardSizeConfig",
            "com.android.camera.utils.watermark.size.StandardSizeConfigTmpl"
        )
        for (clsName in targets) {
            val clazz = runCatching { Class.forName(clsName, false, loader) }.getOrElse {
                HookLogger.log(LogLevel.WARN, TAG, "$clsName not found: $it")
                continue
            }
            val method = runCatching { clazz.getDeclaredMethod("isIqooLogoName") }.getOrElse {
                HookLogger.log(LogLevel.WARN, TAG, "${clazz.name}#isIqooLogoName not found: $it")
                continue
            }
            runCatching {
                module.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept { chain ->
                        if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ZEISS, true)) return@intercept chain.proceed()
                        val origin = chain.proceed() as? Boolean
                        if (origin == true) {
                            HookLogger.log(LogLevel.INFO, TAG, "${clazz.simpleName}#isIqooLogoName: true -> false（ZEISS 需走 vivo 路径）")
                        }
                        false
                    }
                HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#isIqooLogoName -> false")
                count++
            }.onFailure {
                HookLogger.log(LogLevel.WARN, TAG, "hook ${clazz.name}#isIqooLogoName failed: $it")
            }
        }
        return count
    }

    private fun hookDeviceUtil(module: XposedModule, loader: ClassLoader): Int {
        val clazz = runCatching { Class.forName(CLASS_DEVICE_UTIL, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_DEVICE_UTIL not found: $it")
            return 0
        }
        val method = runCatching { clazz.getDeclaredMethod("isIQOO") }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "${clazz.name}#isIQOO not found: $it")
            return 0
        }
        return runCatching {
            module.hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ZEISS, true)) return@intercept chain.proceed()
                    val isWatermarkStack = Thread.currentThread().stackTrace.any {
                        it.className.contains("watermark", ignoreCase = true) || it.className.startsWith("kj.")
                    }
                    if (!isWatermarkStack) return@intercept chain.proceed()
                    val origin = chain.proceed() as? Boolean
                    if (origin == true) {
                        HookLogger.log(LogLevel.INFO, TAG, "DeviceUtil#isIQOO: true -> false（仅水印栈）")
                    }
                    false
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#isIQOO -> false (watermark stack only)")
            1
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook ${clazz.name}#isIQOO failed: $it")
        }.getOrDefault(0)
    }

    private fun hookSystemProperties(module: XposedModule, loader: ClassLoader): Int {
        var count = 0
        val cameraSysProp = runCatching { Class.forName(CLASS_CAMERA_SYS_PROP, false, loader) }.getOrNull()
        if (cameraSysProp != null) {
            for (m in cameraSysProp.declaredMethods.filter { it.name == "get" }) {
                val params = m.parameterTypes
                if (params.size == 2 && params[0] == String::class.java && params[1] == String::class.java) {
                    runCatching {
                        module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                            if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ZEISS, true)) return@intercept chain.proceed()
                            val key = chain.args[0] as? String
                            val def = chain.args[1] as? String
                            val spoof = spoofSystemProp(key)
                            if (spoof != null) {
                                HookLogger.log(LogLevel.INFO, TAG, "SystemProperties.get($key) -> $spoof")
                                return@intercept spoof
                            }
                            chain.proceed()
                        }
                        HookLogger.log(LogLevel.INFO, TAG, "hooked ${cameraSysProp.name}#get(String,String)")
                        count++
                    }
                }
            }
        } else {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_CAMERA_SYS_PROP not found")
        }
        val androidSysProp = runCatching { Class.forName(CLASS_ANDROID_SYS_PROP, false, loader) }.getOrNull()
        if (androidSysProp != null) {
            for (m in androidSysProp.declaredMethods.filter { it.name == "get" }) {
                val params = m.parameterTypes
                if (params.size == 2 && params[0] == String::class.java && params[1] == String::class.java) {
                    runCatching {
                        module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                            if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ZEISS, true)) return@intercept chain.proceed()
                            val key = chain.args[0] as? String
                            val spoof = spoofSystemProp(key)
                            if (spoof != null) return@intercept spoof
                            chain.proceed()
                        }
                        count++
                    }
                }
                if (params.size == 1 && params[0] == String::class.java) {
                    runCatching {
                        module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                            if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ZEISS, true)) return@intercept chain.proceed()
                            val key = chain.args[0] as? String
                            val spoof = spoofSystemProp(key)
                            if (spoof != null) return@intercept spoof
                            chain.proceed()
                        }
                        count++
                    }
                }
            }
            if (count > 0) HookLogger.log(LogLevel.INFO, TAG, "hooked $CLASS_ANDROID_SYS_PROP#get")
        }
        return count
    }

    private fun spoofSystemProp(key: String?): String? = when (key) {
        "ro.product.model.bbk", "ro.product.model", "ro.product.name" -> TARGET_MODEL
        "ro.vivo.market.name", "ro.product.market.name" -> TARGET_MODEL
        "ro.vivo.product.model", "ro.vivo.product.name" -> TARGET_MODEL
        "ro.vivo.product.series" -> TARGET_SERIES
        "ro.product.brand", "ro.product.manufacturer" -> "vivo"
        "ro.vivo.product.platform" -> TARGET_PLATFORM
        else -> null
    }

    private fun hookBuildModel(): Int {
        return runCatching {
            val build = Class.forName("android.os.Build")
            fun setField(name: String, value: String) {
                runCatching {
                    val f = build.getDeclaredField(name)
                    f.isAccessible = true
                    val modifiers = java.lang.reflect.Field::class.java.getDeclaredField("modifiers").apply { isAccessible = true }
                    try { modifiers.setInt(f, f.modifiers and java.lang.reflect.Modifier.FINAL.inv()) } catch (_: Exception) {}
                    f.set(null, value)
                }
            }
            setField("MODEL", TARGET_MODEL)
            setField("PRODUCT", TARGET_MODEL)
            setField("DEVICE", TARGET_MODEL)
            setField("BRAND", "vivo")
            setField("MANUFACTURER", "vivo")
            HookLogger.log(LogLevel.INFO, TAG, "spoofed Build.MODEL/PRODUCT/DEVICE -> $TARGET_MODEL")
            1
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "spoof Build failed: $it")
        }.getOrDefault(0)
    }

    private fun hookFeatureConfigModel(module: XposedModule, loader: ClassLoader): Int {
        var count = 0
        val classesToHook = mutableListOf<Class<*>>()
        runCatching { Class.forName(CLASS_FEATURE_COMMON, false, loader) }.onSuccess { classesToHook.add(it) }
        runCatching {
            val fc = Class.forName(CLASS_FEATURE, false, loader)
            val field = fc.getDeclaredField("instance")
            field.isAccessible = true
            field.get(null)?.javaClass
        }.onSuccess { c -> if (c != null) classesToHook.add(c) }
        for (clazz in classesToHook.distinctBy { it.name }) {
            for ((name, value) in listOf(
                "getMarketName" to TARGET_MODEL,
                "getVivoLogoName" to TARGET_MODEL,
                "productSeries" to TARGET_SERIES
            )) {
                val m = runCatching { clazz.getDeclaredMethod(name) }.getOrNull() ?: continue
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ZEISS, true)) return@intercept chain.proceed()
                        HookLogger.log(LogLevel.INFO, TAG, "${clazz.simpleName}#$name -> $value")
                        value
                    }
                    HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#$name -> $value")
                    count++
                }
            }
            runCatching { clazz.getDeclaredMethod("productBatchTime") }.getOrNull()?.let { m ->
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ZEISS, true)) return@intercept chain.proceed()
                        TARGET_BATCH_TIME
                    }
                    count++
                }
            }
        }
        return count
    }

    private fun hookWatermarkUtils(module: XposedModule, loader: ClassLoader): Int {
        val clazz = runCatching { Class.forName(CLASS_WATERMARK_UTILS, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_WATERMARK_UTILS not found: $it")
            return 0
        }
        var count = 0
        runCatching { clazz.getDeclaredMethod("generateLogoText", Boolean::class.javaPrimitiveType) }.onSuccess { m ->
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ZEISS, true)) return@intercept chain.proceed()
                    val wmLogoClazz = Class.forName("com.android.camera.utils.watermark.WMLogo", false, loader)
                    val ctor = wmLogoClazz.getDeclaredConstructor(String::class.java)
                    ctor.newInstance(TARGET_MODEL)
                }
                HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#generateLogoText -> $TARGET_MODEL")
                count++
            }
        }
        runCatching { clazz.getDeclaredMethod("getVivoLogoName") }.onSuccess { m ->
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ZEISS, true)) return@intercept chain.proceed()
                    TARGET_MODEL
                }
                count++
            }
        }
        return count
    }

    private companion object {
        const val TAG = "zeiss"
        const val CLASS_FEATURE = "com.android.camera.featureconfig.FeatureConfig"
        const val CLASS_FEATURE_COMMON = "com.android.camera.featureconfig.FeatureConfig_common"
        const val CLASS_DEVICE_UTIL = "com.android.camera.utils.DeviceUtil"
        const val CLASS_CAMERA_SYS_PROP = "com.android.camera.utils.SystemProperties"
        const val CLASS_ANDROID_SYS_PROP = "android.os.SystemProperties"
        const val CLASS_WATERMARK_UTILS = "com.android.camera.utils.watermark.WatermarkUtils"
        const val TARGET_MODEL = "vivo X500 BETA"
        const val TARGET_SERIES = "X"
        const val TARGET_PLATFORM = "MT6991"
        const val TARGET_BATCH_TIME = 20250930
    }
}
