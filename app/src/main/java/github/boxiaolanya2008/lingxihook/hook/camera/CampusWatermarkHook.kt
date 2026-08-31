package github.boxiaolanya2008.lingxihook.hook.camera

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * 校园水印修复（Hook 点来自 jadx 反编译 com.android.camera 源码）。
 *
 * 故障：
 * - 模板 gk/a.java GRADUATE_SCHOOL 在非 vivo 校园机型上虽已在静态池中，但
 *   选择后 oi/f.java:165 beforeOnItemClick 检测到 KEY_WATER_MARK_GRADUATE_SCHOOL 为空
 *   会弹学校选择对话框并拦截点击，导致用户感觉“选了没用”。
 * - 即使选中，nj/e.java 等生成侧读取该 key 若为空或 "normal" 会按
 *   RelatedWMItem 的 unShowList 隐藏大部分元素，最终位图为空或仅边框，表现为“拍出来没有校园水印”。
 *
 * 方案：已开启时
 * 1) 拦截所有 getSettingValueFromKey(ISettingKeys.KEY_WATER_MARK_GRADUATE_SCHOOL) 调用，
 *    若原值为空/“normal”/“unknown”则返回默认 “浙江大学”，确保后续按浙大资源（j.f43224ph 等）正常渲染；
 * 2) 拦截 oi/f#beforeOnItemClick，对 GRADUATE_SCHOOL 模板若检测到学校为空则自动写入 “浙江大学”
 *    并放行点击，不再弹空学校拦截；
 * 3) 对已建好的 WMTemplate 池中 GRADUATE_SCHOOL 的 RelatedWMItem unShowList 已在 WatermarkIconHook 中清空，
 *    此处不重复，仅保证学校名非空。
 */
class CampusWatermarkHook {

    fun install(module: XposedModule, param: PackageLoadedParam) {
        val loader = param.defaultClassLoader
        var hooked = 0
        hooked += hookSettingRead(module, loader)
        hooked += hookBeforeOnItemClick(module, loader)
        if (hooked == 0) {
            HookLogger.log(LogLevel.WARN, TAG, "no campus methods hooked, maybe obfuscation changed")
        } else {
            HookLogger.log(LogLevel.INFO, TAG, "installed ($hooked hooks)")
        }
    }

    private fun hookSettingRead(module: XposedModule, loader: ClassLoader): Int {
        var count = 0
        val candidates = listOf(
            "com.android.camera.setting.api.ISettingManager",
            "com.android.camera.setting.impl.SettingManager",
            "com.android.camera.setting.SettingManager",
            "com.vivo.camera.setting.SettingManager",
            "com.android.camera.data.SettingManager"
        )
        val keyName = "pref_camera_watermark_graduate_school"
        for (clsName in candidates) {
            val clazz = runCatching { Class.forName(clsName, false, loader) }.getOrNull() ?: continue
            for (m in clazz.declaredMethods.filter { it.name == "getSettingValueFromKey" }) {
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_CAMPUS, true)) return@intercept chain.proceed()
                        val key = chain.args.getOrNull(0) as? String
                        val result = chain.proceed()
                        if (key != null && (key == KEY_GRADUATE_SCHOOL || key.contains("GRADUATE_SCHOOL") || key == keyName)) {
                            val str = result as? String
                            if (str.isNullOrBlank() || str == "normal" || str == "unknown" || str == "7") {
                                HookLogger.log(LogLevel.INFO, TAG, "getSettingValueFromKey($key): $str -> $DEFAULT_SCHOOL")
                                return@intercept DEFAULT_SCHOOL
                            }
                        }
                        result
                    }
                    HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#getSettingValueFromKey")
                    count++
                }
            }
        }
        if (count == 0) {
            HookLogger.log(LogLevel.WARN, TAG, "no ISettingManager#getSettingValueFromKey found, try generic scan")
            runCatching {
                val all = mutableSetOf<Class<*>>()
                for (name in candidates) {
                    runCatching { Class.forName(name, false, loader) }.onSuccess { all.add(it) }
                }
                for (c in all) {
                    for (m in c.methods.filter { it.name == "getSettingValueFromKey" }) {
                        runCatching {
                            module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                                if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_CAMPUS, true)) return@intercept chain.proceed()
                                val key = chain.args.getOrNull(0) as? String
                                val result = chain.proceed()
                                if (key?.contains("GRADUATE") == true) {
                                    val str = result as? String
                                    if (str.isNullOrBlank() || str == "normal" || str == "7") {
                                        return@intercept DEFAULT_SCHOOL
                                    }
                                }
                                result
                            }
                            count++
                        }
                    }
                }
            }
        }
        return count
    }

    private fun hookBeforeOnItemClick(module: XposedModule, loader: ClassLoader): Int {
        val clazz = runCatching { Class.forName(CLASS_TMPL_MANAGER, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_TMPL_MANAGER not found: $it")
            return 0
        }
        val method = runCatching { clazz.getDeclaredMethod("beforeOnItemClick", Int::class.javaPrimitiveType) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "${clazz.name}#beforeOnItemClick not found: $it")
            return 0
        }
        return runCatching {
            module.hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                if (!HookConfig.isEnabled(VivoCameraHook.FEATURE_CAMPUS, true)) return@intercept chain.proceed()
                val self = chain.thisObject
                val pos = chain.args[0] as? Int ?: return@intercept chain.proceed()
                runCatching {
                    val managerField = clazz.getDeclaredField("f55905m")
                    managerField.isAccessible = true
                    val settingManager = managerField.get(self)
                    val getMethod = settingManager.javaClass.methods.firstOrNull { it.name == "getSettingValueFromKey" }
                    val changeMethod = settingManager.javaClass.methods.firstOrNull { it.name == "changeSetting" || it.name == "setSettingValue" || it.name == "putSettingValue" }
                    val keyField = Class.forName("com.android.camera.featureconfig.configuration.database.ISettingKeys", false, loader)
                        .getDeclaredField("KEY_WATER_MARK_GRADUATE_SCHOOL").get(null) as? String
                    val schoolKey = keyField ?: KEY_GRADUATE_SCHOOL
                    val cur = getMethod?.invoke(settingManager, schoolKey, String::class.java) as? String
                        ?: getMethod?.invoke(settingManager, schoolKey, *arrayOf<Class<*>>(String::class.java)) as? String
                    if (cur.isNullOrBlank() || cur == "normal" || cur == "7") {
                        HookLogger.log(LogLevel.INFO, TAG, "beforeOnItemClick school empty $cur -> $DEFAULT_SCHOOL, auto set")
                        if (changeMethod != null) {
                            runCatching { changeMethod.invoke(settingManager, schoolKey, DEFAULT_SCHOOL) }
                            runCatching { changeMethod.invoke(settingManager, schoolKey, DEFAULT_SCHOOL, String::class.java) }
                        } else {
                            val m2 = settingManager.javaClass.getDeclaredMethod("changeSetting", String::class.java, String::class.java)
                            m2.invoke(settingManager, schoolKey, DEFAULT_SCHOOL)
                        }
                        val listMethod = clazz.getDeclaredMethod("getManagerDataList")
                        listMethod.isAccessible = true
                        val list = listMethod.invoke(self) as? List<*>
                        val tmpl = list?.getOrNull(pos)?.let { item ->
                            runCatching { item.javaClass.getMethod("o").invoke(item) }.getOrNull()
                        }
                        val tmplId = runCatching { tmpl?.javaClass?.getMethod("getId")?.invoke(tmpl) as? String }.getOrNull()
                        if (tmplId == "GRADUATE_SCHOOL") {
                            return@intercept false
                        }
                    }
                }
                chain.proceed()
            }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#beforeOnItemClick -> auto set school")
            1
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook beforeOnItemClick failed: $it")
        }.getOrDefault(0)
    }

    private companion object {
        const val TAG = "campus"
        const val CLASS_TMPL_MANAGER = "oi.f"
        const val KEY_GRADUATE_SCHOOL = "pref_camera_watermark_graduate_school"
        const val DEFAULT_SCHOOL = "浙江大学"
    }
}
