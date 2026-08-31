package github.boxiaolanya2008.lingxihook.hook.abe

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.AppHooker
import github.boxiaolanya2008.lingxihook.hook.HookFeature
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * com.vivo.abe（智慧引擎）适配入口。
 * 系统名称“智慧引擎”，包名 com.vivo.abe，常驻多进程，含静默重启、高耗电管控、后台清理等能力。
 */
class VivoAbeHook : AppHooker {

    override val packageName = "com.vivo.abe"
    override val label = "智慧引擎（vivo ABE）"
    override val description =
        "vivo 智慧引擎（Smart Engine），包名 com.vivo.abe，主进程常驻，含静默重启、高耗电管控、后台清理等能力"

    override val features = listOf(
        HookFeature(
            key = FEATURE_SILENT_REBOOT,
            title = "屏蔽自动重启",
            description = "拦截 SilentRebootService#p0/o0/g0/v0 夜间静默重启调度与 PowerManager.reboot(silent/reboot) 调用，" +
                "并阻断 e4.a#i 异常策略重启，02:00-04:00 不再自动重启，需 Root 验证仅用于日志说明。",
            defaultEnabled = true
        )
    )

    private val silentRebootHook = SilentRebootHook()

    override fun install(module: XposedModule, param: PackageLoadedParam) {
        HookLogger.log(LogLevel.INFO, "abe", "适配器已注入：${param.packageName}")
        silentRebootHook.install(module, param)
    }

    companion object {
        /** 静默重启屏蔽开关持久化键 */
        const val FEATURE_SILENT_REBOOT = "lingxi_hook_abe_silent_reboot"
    }
}
