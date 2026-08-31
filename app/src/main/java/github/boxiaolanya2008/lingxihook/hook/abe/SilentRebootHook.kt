package github.boxiaolanya2008.lingxihook.hook.abe

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * 静默重启屏蔽（Hook 点来自 jadx 反编译 com.vivo.abe 5.16.0.7）。
 *
 * 路径：
 * - com.vivo.silentreboot.SilentRebootService#p0(String) 最终 PowerManager.reboot("silent")，o0() 为 02:00-04:00 判定，g0/v0/n0 为 Alarm 调度
 * - android.os.PowerManager#reboot(String) 框架入口，ABE 两个 reason "silent"/"reboot"
 * - e4.a#i() 异常策略 sysrb 直接 reboot("reboot")
 *
 * 方案：已开启时
 * 1) 拦截 SilentRebootService 全部调度方法直接 return，不建 Alarm，不进 o0 判定
 * 2) 拦截 PowerManager.reboot(String) 遇 silent/reboot 直接拦截不执行
 * 3) 拦截 e4.a#i() 直接 return
 * 关闭开关走原逻辑。
 */
class SilentRebootHook {

    fun install(module: XposedModule, param: PackageLoadedParam) {
        val loader = param.defaultClassLoader
        var hooked = 0
        hooked += hookSilentRebootService(module, loader)
        hooked += hookPowerManagerReboot(module, loader)
        hooked += hookExceptionReboot(module, loader)
        if (hooked == 0) {
            HookLogger.log(LogLevel.WARN, TAG, "no silent reboot methods hooked, maybe obfuscation changed")
        } else {
            HookLogger.log(LogLevel.INFO, TAG, "installed ($hooked hooks) silent reboot blocked")
        }
    }

    private fun hookSilentRebootService(module: XposedModule, loader: ClassLoader): Int {
        val clazz = runCatching { Class.forName(CLASS_SILENT, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_SILENT not found: $it")
            return 0
        }
        var count = 0
        val methodsToBlock = setOf("p0", "o0", "g0", "v0", "n0", "Z", "Y", "X", "u0", "a0")
        for (m in clazz.declaredMethods) {
            if (m.name !in methodsToBlock) continue
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoAbeHook.FEATURE_SILENT_REBOOT, true)) return@intercept chain.proceed()
                    val methodName = m.name
                    HookLogger.log(LogLevel.INFO, TAG, "${clazz.simpleName}#$methodName blocked (auto reboot off)")
                    when (m.returnType) {
                        java.lang.Boolean.TYPE, java.lang.Boolean::class.java -> false
                        java.lang.Integer.TYPE, java.lang.Integer::class.java -> 0
                        java.lang.Long.TYPE, java.lang.Long::class.java -> 0L
                        else -> null
                    }
                }
                HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#${m.name} -> blocked")
                count++
            }.onFailure {
                HookLogger.log(LogLevel.WARN, TAG, "hook ${clazz.name}#${m.name} failed: $it")
            }
        }
        // 兜底：按特征签名再扫一次 p0(String) / o0() 防止方法被重命名但参数特征仍匹配
        if (count == 0) {
            for (m in clazz.declaredMethods) {
                val params = m.parameterTypes
                val isP0 = params.size == 1 && params[0] == String::class.java && m.returnType == Void.TYPE
                val isO0 = params.isEmpty() && m.returnType == Void.TYPE
                if (!isP0 && !isO0) continue
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoAbeHook.FEATURE_SILENT_REBOOT, true)) return@intercept chain.proceed()
                        HookLogger.log(LogLevel.INFO, TAG, "${clazz.simpleName}#${m.name}(fallback) blocked")
                        null
                    }
                    count++
                }
            }
        }
        return count
    }

    private fun hookPowerManagerReboot(module: XposedModule, loader: ClassLoader): Int {
        val pm = runCatching { Class.forName("android.os.PowerManager", false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "PowerManager not found: $it")
            return 0
        }
        var count = 0
        for (m in pm.declaredMethods.filter { it.name == "reboot" }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoAbeHook.FEATURE_SILENT_REBOOT, true)) return@intercept chain.proceed()
                    val reason = chain.args.getOrNull(0) as? String ?: ""
                    val isAbeReboot = reason == "silent" || reason == "reboot" || reason.contains("silent", true)
                    if (!isAbeReboot) return@intercept chain.proceed()
                    val stack = Thread.currentThread().stackTrace.joinToString("|") { it.className }
                    val fromAbe = stack.contains("silentreboot", true) || stack.contains("abe", true) || stack.contains("e4.a")
                    if (!fromAbe && reason != "silent" && reason != "reboot") return@intercept chain.proceed()
                    HookLogger.log(LogLevel.INFO, TAG, "PowerManager#reboot($reason) blocked (from ABE, stack silentreboot)")
                    null
                }
                HookLogger.log(LogLevel.INFO, TAG, "hooked PowerManager#reboot -> block silent/reboot")
                count++
            }.onFailure {
                HookLogger.log(LogLevel.WARN, TAG, "hook PowerManager#reboot failed: $it")
            }
        }
        return count
    }

    private fun hookExceptionReboot(module: XposedModule, loader: ClassLoader): Int {
        val clazz = runCatching { Class.forName(CLASS_EXCEPTION_IMPL, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_EXCEPTION_IMPL not found: $it")
            return 0
        }
        var count = 0
        for (m in clazz.declaredMethods.filter { it.name == "i" && it.parameterTypes.isEmpty() }) {
            runCatching {
                module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                    if (!HookConfig.isEnabled(VivoAbeHook.FEATURE_SILENT_REBOOT, true)) return@intercept chain.proceed()
                    HookLogger.log(LogLevel.INFO, TAG, "e4.a#i() sysrb reboot blocked")
                    null
                }
                HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#i -> blocked")
                count++
            }
        }
        // 兼容：e4 类可能被混淆位移，扫所有无参 void 方法名含 reboot 特征
        if (count == 0) {
            for (m in clazz.declaredMethods.filter { it.returnType == Void.TYPE && it.parameterTypes.isEmpty() }) {
                runCatching {
                    module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                        if (!HookConfig.isEnabled(VivoAbeHook.FEATURE_SILENT_REBOOT, true)) return@intercept chain.proceed()
                        val name = m.name
                        if (name == "i" || name.contains("reboot", true)) {
                            HookLogger.log(LogLevel.INFO, TAG, "${clazz.name}#$name blocked")
                            return@intercept null
                        }
                        chain.proceed()
                    }
                }
            }
        }
        return count
    }

    private companion object {
        const val TAG = "abe"
        const val CLASS_SILENT = "com.vivo.silentreboot.SilentRebootService"
        const val CLASS_EXCEPTION_IMPL = "e4.a"
    }
}
