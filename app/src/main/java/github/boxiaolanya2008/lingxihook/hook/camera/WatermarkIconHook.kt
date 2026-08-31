package github.boxiaolanya2008.lingxihook.hook.camera

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * 水印图标全显（Hook 点来自 jadx 反编译 com.android.camera 源码）。
 *
 * 过滤链：
 * - gk/j.java 静态池在类加载时按机型生成 WMTemplate，每个 WMTemplate.WMItem 的
 *   unShowList（HashSet<Integer>）为“需隐藏的图标下标”，如 BORDER 模板默认
 *   new HashSet(2,3,4,5) 隐藏 4 个，只留其余；FeatureConfig_meat_* 覆写
 *   isSupportGoldMaterial()/isSupportShowWatermarkIcon() 等决定集合大小。
 * - FeatureConfig_common#isSupportShowWatermarkIcon() 受 DeviceUtil.isYSeries /
 *   isTSeries / isPadDevice 影响，Y/T 系列直接隐藏图标栏。
 * - FeatureConfig_common#getIQOOBorderWatermarkImageOrder()/getIQOOBorderWatermarkImageVersion()
 *   决定边框水印可选的 IQOO/KPL/三色图标数量。
 *
 * 方案：已开启时
 * 1) 把 isSupportShowWatermarkIcon 强制 true，放行图标栏；
 * 2) 把所有 WMItem / RelatedWMItem 的 unShowList 清空（空集 = 全部显示）并把 show/enable 置 true；
 * 3) 把 IQOO 边框版本强制 2（3 图标：THREECOLOR/IQOO/KPL）并返回全量列表；
 * 4) 对已建好的 gk/j 静态池 Map<String,WMTemplate> 做反射补丁，兼容类已加载场景。
 */
class WatermarkIconHook {

    fun install(module: XposedModule, param: PackageLoadedParam) {
        val loader = param.defaultClassLoader
        var hooked = 0
        hooked += hookFeatureGates(module, loader)
        hooked += hookWMItemConstructors(module, loader)
        hooked += hookIQOOOrder(module, loader)
        hooked += patchExistingPool(loader)
        if (hooked == 0) {
            HookLogger.log(LogLevel.WARN, TAG, "no icon methods hooked, maybe obfuscation changed")
        } else {
            HookLogger.log(LogLevel.INFO, TAG, "installed ($hooked hooks)")
        }
    }

    private fun hookFeatureGates(module: XposedModule, loader: ClassLoader): Int {
        var count = 0
        val classesToHook = mutableListOf<Class<*>>()
        runCatching { Class.forName(CLASS_FEATURE_COMMON, false, loader) }.onSuccess { classesToHook.add(it) }
        runCatching {
            val fc = Class.forName(CLASS_FEATURE, false, loader)
            val f = fc.getDeclaredField("instance")
            f.isAccessible = true
            f.get(null)?.javaClass
        }.onSuccess { c -> if (c != null && classesToHook.none { it.name == c.name }) classesToHook.add(c) }

        for (clazz in classesToHook.distinctBy { it.name }) {
            runCatching { clazz.getDeclaredMethod("isSupportShowWatermarkIcon") }.onSuccess { m ->
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ICONS, true)) return@intercept chain.proceed()
                        val origin = chain.proceed() as? Boolean
                        if (origin != true) HookLogger.log(LogLevel.INFO, TAG, "${clazz.simpleName}#isSupportShowWatermarkIcon: $origin -> true")
                        true
                    }
                    HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#isSupportShowWatermarkIcon -> true")
                    count++
                }
            }
            runCatching { clazz.getDeclaredMethod("isSupportGoldMaterial") }.onSuccess { m ->
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ICONS, true)) return@intercept chain.proceed()
                        true
                    }
                    count++
                }
            }
            runCatching { clazz.getDeclaredMethod("isSupportWatermarkBorder") }.onSuccess { m ->
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ICONS, true)) return@intercept chain.proceed()
                        true
                    }
                    count++
                }
            }
        }
        return count
    }

    private fun hookWMItemConstructors(module: XposedModule, loader: ClassLoader): Int {
        var count = 0
        val wmItem = runCatching { Class.forName(CLASS_WMITEM, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_WMITEM not found: $it")
            return 0
        }
        val relatedItem = runCatching { Class.forName(CLASS_RELATED, false, loader) }.getOrNull()

        for (ctor in wmItem.declaredConstructors) {
            val hasSet = ctor.parameterTypes.any { it == java.util.Set::class.java || it.name == "java.util.HashSet" }
            if (!hasSet) continue
            runCatching {
                module.hook(ctor).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    val obj = chain.proceed()
                    if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ICONS, true)) return@intercept obj
                    runCatching {
                        val f = wmItem.getDeclaredField("unShowList")
                        f.isAccessible = true
                        f.set(obj, HashSet<Int>())
                        val showF = wmItem.getDeclaredField("show")
                        showF.isAccessible = true
                        showF.setBoolean(obj, true)
                    }
                    obj
                }
                count++
            }
        }
        if (count > 0) HookLogger.log(LogLevel.INFO, TAG, "hooked ${wmItem.name} constructors ($count) -> unShowList empty")

        relatedItem?.let { clazz ->
            for (ctor in clazz.declaredConstructors) {
                val hasSet = ctor.parameterTypes.any { it == java.util.Set::class.java }
                if (!hasSet) continue
                runCatching {
                    module.hook(ctor).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        val obj = chain.proceed()
                        if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ICONS, true)) return@intercept obj
                        runCatching {
                            val f = clazz.getDeclaredField("unShowList")
                            f.isAccessible = true
                            f.set(obj, HashSet<Int>())
                        }
                        obj
                    }
                    count++
                }
            }
        }
        return count
    }

    private fun hookIQOOOrder(module: XposedModule, loader: ClassLoader): Int {
        var count = 0
        val classesToHook = mutableListOf<Class<*>>()
        runCatching { Class.forName(CLASS_FEATURE_COMMON, false, loader) }.onSuccess { classesToHook.add(it) }
        runCatching {
            val fc = Class.forName(CLASS_FEATURE, false, loader)
            val f = fc.getDeclaredField("instance")
            f.isAccessible = true
            f.get(null)?.javaClass
        }.onSuccess { c -> if (c != null) classesToHook.add(c) }

        for (clazz in classesToHook.distinctBy { it.name }) {
            runCatching { clazz.getDeclaredMethod("getIQOOBorderWatermarkImageVersion") }.onSuccess { m ->
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ICONS, true)) return@intercept chain.proceed()
                        2
                    }
                    count++
                }
            }
            runCatching { clazz.getDeclaredMethod("getIQOOBorderWatermarkImageOrder") }.onSuccess { m ->
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_ICONS, true)) return@intercept chain.proceed()
                        @Suppress("UNCHECKED_CAST")
                        val origin = chain.proceed() as? List<String>
                        val full = listOf("threecolor_logo", "iqoo_logo", "kpl_logo")
                        if (origin == null || origin.size < full.size) {
                            HookLogger.log(LogLevel.INFO, TAG, "${clazz.simpleName}#getIQOOBorderWatermarkImageOrder: ${origin?.size ?: 0} -> ${full.size}")
                        }
                        full
                    }
                    count++
                }
            }
        }
        if (count > 0) HookLogger.log(LogLevel.INFO, TAG, "hooked IQOO order/version ($count)")
        return count
    }

    private fun patchExistingPool(loader: ClassLoader): Int {
        return runCatching {
            val pool = Class.forName(CLASS_TMPL_POOL, false, loader)
            var patched = 0
            var injected = 0
            for (field in pool.declaredFields) {
                if (field.type != java.util.Map::class.java) continue
                field.isAccessible = true
                val map = field.get(null) as? Map<*, *> ?: continue
                for (value in map.values) {
                    if (value == null) continue
                    val tmplClass = value.javaClass
                    val id = runCatching { tmplClass.getMethod("getId").invoke(value) as? String }.getOrNull()
                    val itemsField = runCatching { tmplClass.getDeclaredField("items") }.getOrNull() ?: continue
                    itemsField.isAccessible = true
                    @Suppress("UNCHECKED_CAST")
                    val items = itemsField.get(value) as? MutableList<Any> ?: continue
                    for (item in items.toList()) {
                        if (item == null) continue
                        val itemClass = item.javaClass
                        if (itemClass.name != CLASS_WMITEM) continue
                        runCatching {
                            val f = itemClass.getDeclaredField("unShowList")
                            f.isAccessible = true
                            val cur = f.get(item) as? Set<*>
                            if (cur != null && cur.isNotEmpty()) {
                                f.set(item, HashSet<Int>())
                                patched++
                            }
                            val showF = itemClass.getDeclaredField("show")
                            showF.isAccessible = true
                            showF.setBoolean(item, true)
                        }
                        runCatching {
                            val relatedF = itemClass.getDeclaredField("relatedWMItem")
                            relatedF.isAccessible = true
                            val list = relatedF.get(item) as? List<*>
                            list?.forEach { rel ->
                                if (rel == null) return@forEach
                                runCatching {
                                    val rf = rel.javaClass.getDeclaredField("unShowList")
                                    rf.isAccessible = true
                                    rf.set(rel, HashSet<Int>())
                                }
                            }
                        }
                    }
                    if (HookConfig.isEnabled(VivoCameraHook.FEATURE_ICONS, true)) {
                        val needsIcon = id == "BORDER_PHOTO" || id == "BORDER_PHOTO_AURALIGHT"
                        if (needsIcon) {
                            val hasLogoPic = items.any {
                                runCatching {
                                    val k = it.javaClass.getDeclaredField("key").get(it) as? String
                                    k == "pref_camera_water_mark_logo_pic" || k == "KEY_WATER_MARK_LOGO_PIC" || (k?.contains("LOGO_PIC") == true)
                                }.getOrDefault(false) || runCatching {
                                    val keyF = it.javaClass.getDeclaredField("key")
                                    keyF.isAccessible = true
                                    val keyVal = keyF.get(it) as? String
                                    keyVal == "pref_camera_water_mark_logo_pic"
                                }.getOrDefault(false)
                            } || items.any {
                                runCatching {
                                    val typeF = it.javaClass.getDeclaredField("type")
                                    typeF.isAccessible = true
                                    val typeVal = typeF.get(it)
                                    typeVal.toString().contains("LOGO_PIC")
                                }.getOrDefault(false)
                            }
                            if (!hasLogoPic) {
                                runCatching {
                                    val wmItemClass = Class.forName(CLASS_WMITEM, false, loader)
                                    val wmItemIdClass = Class.forName("com.android.camera.constant.watermark.WMItemID", false, loader)
                                    val logoPicId = java.lang.Enum.valueOf(wmItemIdClass as Class<out Enum<*>>, "LOGO_PIC")
                                    val ctor = wmItemClass.getDeclaredConstructor(
                                        Int::class.javaPrimitiveType,
                                        String::class.java, String::class.java, String::class.java, String::class.java,
                                        wmItemIdClass, String::class.java, String::class.java,
                                        Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
                                        String::class.java, String::class.java
                                    )
                                    val newItem = ctor.newInstance(
                                        0,
                                        "-1",
                                        "R.array.pref_camera_watermark_boder_logo_pics",
                                        "R.array.pref_camera_watermark_boder_logo_pics",
                                        "R.string.watermark_icon_item",
                                        logoPicId,
                                        "pref_camera_water_mark_logo_pic",
                                        "1",
                                        true,
                                        true,
                                        "-1",
                                        ""
                                    )
                                    items.add(newItem)
                                    injected++
                                    HookLogger.log(LogLevel.INFO, TAG, "injected LOGO_PIC for $id")
                                }
                            }
                        }
                    }
                }
            }
            if (patched > 0) HookLogger.log(LogLevel.INFO, TAG, "patched existing pool $patched items -> unShowList empty")
            if (injected > 0) HookLogger.log(LogLevel.INFO, TAG, "injected $injected LOGO_PIC items for border")
            if (patched > 0 || injected > 0) 1 else 0
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "patch pool failed: $it")
        }.getOrDefault(0)
    }

    private companion object {
        const val TAG = "icons"
        const val CLASS_FEATURE = "com.android.camera.featureconfig.FeatureConfig"
        const val CLASS_FEATURE_COMMON = "com.android.camera.featureconfig.FeatureConfig_common"
        const val CLASS_WMITEM = "com.android.camera.featureconfig.configuration.watermark.WMTemplate\$WMItem"
        const val CLASS_RELATED = "com.android.camera.featureconfig.configuration.watermark.WMTemplate\$RelatedWMItem"
        const val CLASS_TMPL_POOL = "gk.j"
    }
}
