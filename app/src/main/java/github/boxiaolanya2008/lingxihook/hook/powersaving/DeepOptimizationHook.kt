package github.boxiaolanya2008.lingxihook.hook.powersaving

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * 应用深度优化（Dexopt）关闭 Hook
 *
 * 入口：com.iqoo.powersaving/.activity.AppOptimizationActivity (action app_depth_optimization.action)
 * 实测 dex 中：
 * - com.iqoo.powersaving.appoptimize.b (单例 b()) 负责调度：startDexoptJob(StartDexoptCallback):Z / getDexoptPackages():List / getPredictDexoptTime(List):J / getRunningStatus():DexoptStatus
 * - com.iqoo.powersaving.service.AppDexOptService 前台调度
 * - 系统属性 persist.vivo.on_demand_dexopt.support 为总开关（可选另钩 SystemProperties）
 *
 * 方案：已开启时让调度直接失败/空列表/已优化态，入口按钮点后无事发生，后台自动任务也不触发。
 * 关闭开关则走原逻辑。
 */
class DeepOptimizationHook {

    fun install(module: XposedModule, param: PackageLoadedParam) {
        val loader = param.defaultClassLoader
        val targets = listOf(
            "com.iqoo.powersaving.appoptimize.b",
            "com.iqoo.powersaving.appoptimize.d"
        )
        var hooked = 0
        for (clsName in targets) {
            val clazz = runCatching { Class.forName(clsName, false, loader) }.getOrElse {
                HookLogger.log(LogLevel.WARN, TAG, "$clsName not found: $it")
                continue
            }
            hooked += hookStartDexoptJob(module, clazz, loader)
            hooked += hookGetDexoptPackages(module, clazz)
            hooked += hookGetPredictTime(module, clazz)
            hooked += hookGetRunningStatus(module, clazz, loader)
        }
        if (hooked == 0) {
            HookLogger.log(LogLevel.WARN, TAG, "no dexopt methods hooked, maybe obfuscation changed")
        } else {
            HookLogger.log(LogLevel.INFO, TAG, "installed ($hooked hooks)")
        }
    }

    private fun shouldBlock(): Boolean =
        HookConfig.isEnabled(IqooPowerSavingHook.FEATURE_DEEPOPT, true).not().not()
            // isEnabled=true → 需要拦截；false → 放行原逻辑
            .let { enabled -> enabled }

    private fun hookStartDexoptJob(module: XposedModule, clazz: Class<*>, loader: ClassLoader): Int {
        val cb = runCatching { Class.forName("com.iqoo.powersaving.appoptimize.StartDexoptCallback", false, loader) }.getOrNull()
            ?: runCatching { Class.forName("com.iqoo.powersaving.appoptimize.a\$c", false, loader) }.getOrNull()
        if (cb == null) {
            HookLogger.log(LogLevel.WARN, TAG, "${clazz.name}#startDexoptJob callback class not found")
            return 0
        }
        val m = runCatching { clazz.getDeclaredMethod("startDexoptJob", cb) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "${clazz.name}#startDexoptJob not found: $it")
            return 0
        }
        return runCatching {
            module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                if (!HookConfig.isEnabled(IqooPowerSavingHook.FEATURE_DEEPOPT, true)) return@intercept chain.proceed()
                HookLogger.log(LogLevel.INFO, TAG, "${clazz.simpleName}#startDexoptJob blocked (deepopt already off)")
                false
            }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#startDexoptJob -> false")
            1
        }.onFailure { HookLogger.log(LogLevel.WARN, TAG, "hook ${clazz.name}#startDexoptJob failed: $it") }.getOrDefault(0)
    }

    private fun hookGetDexoptPackages(module: XposedModule, clazz: Class<*>): Int {
        val m = runCatching { clazz.getDeclaredMethod("getDexoptPackages") }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "${clazz.name}#getDexoptPackages not found: $it")
            return 0
        }
        return runCatching {
            module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                if (!HookConfig.isEnabled(IqooPowerSavingHook.FEATURE_DEEPOPT, true)) return@intercept chain.proceed()
                val origin = chain.proceed() as? List<*>
                if (!origin.isNullOrEmpty()) {
                    HookLogger.log(LogLevel.INFO, TAG, "${clazz.simpleName}#getDexoptPackages: ${origin.size} -> 0 (blocked)")
                }
                emptyList<Any>()
            }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#getDexoptPackages -> empty")
            1
        }.onFailure { HookLogger.log(LogLevel.WARN, TAG, "hook ${clazz.name}#getDexoptPackages failed: $it") }.getOrDefault(0)
    }

    private fun hookGetPredictTime(module: XposedModule, clazz: Class<*>): Int {
        val m = runCatching { clazz.getDeclaredMethod("getPredictDexoptTime", MutableList::class.java) }
            .getOrElse { runCatching { clazz.getDeclaredMethod("getPredictDexoptTime", List::class.java) }.getOrNull() }
            ?: runCatching { clazz.methods.firstOrNull { it.name == "getPredictDexoptTime" } } .getOrNull()
        if (m == null) {
            HookLogger.log(LogLevel.WARN, TAG, "${clazz.name}#getPredictDexoptTime not found")
            return 0
        }
        return runCatching {
            module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                if (!HookConfig.isEnabled(IqooPowerSavingHook.FEATURE_DEEPOPT, true)) return@intercept chain.proceed()
                0L
            }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#getPredictDexoptTime -> 0")
            1
        }.onFailure { HookLogger.log(LogLevel.WARN, TAG, "hook ${clazz.name}#getPredictDexoptTime failed: $it") }.getOrDefault(0)
    }

    private fun hookGetRunningStatus(module: XposedModule, clazz: Class<*>, loader: ClassLoader): Int {
        val m = runCatching { clazz.getDeclaredMethod("getRunningStatus") }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "${clazz.name}#getRunningStatus not found: $it")
            return 0
        }
        return runCatching {
            module.hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                if (!HookConfig.isEnabled(IqooPowerSavingHook.FEATURE_DEEPOPT, true)) return@intercept chain.proceed()
                val origin = chain.proceed()
                // 枚举 com.iqoo.powersaving.appoptimize.IAppOptimizeInterface$DexoptStatus.OPTIMIZED_MANUAL 视为已优化，不再触发
                val optimized = runCatching {
                    val e = Class.forName("com.iqoo.powersaving.appoptimize.IAppOptimizeInterface\$DexoptStatus", false, loader)
                    java.lang.Enum.valueOf(e as Class<out Enum<*>>, "OPTIMIZED_MANUAL")
                }.getOrNull() ?: origin
                if (origin != optimized) {
                    HookLogger.log(LogLevel.INFO, TAG, "${clazz.simpleName}#getRunningStatus: $origin -> OPTIMIZED_MANUAL")
                }
                optimized
            }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}#getRunningStatus -> OPTIMIZED_MANUAL")
            1
        }.onFailure { HookLogger.log(LogLevel.WARN, TAG, "hook ${clazz.name}#getRunningStatus failed: $it") }.getOrDefault(0)
    }

    private companion object {
        const val TAG = "deepopt"
    }
}
