package github.boxiaolanya2008.lingxihook.hook.device

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.AppHooker
import github.boxiaolanya2008.lingxihook.hook.HookFeature
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * system_server（android 作用域）适配入口：真实电量与更新屏蔽。
 * 原机型伪装（PD2520→PD2502 / V2520A→V2502A）已移除——system_server 全局伪装
 * 会导致云配置按错误机型下发、daemon 名单重灌错乱，得不偿失。
 */
class SystemHook : AppHooker {
    override val packageName = "android"
    override val label = "系统（真实电量 / 更新屏蔽）"
    override val description = "拦截 system_server 电池广播使电量显示去虚电；更新屏蔽需 Root 手动 setprop"
    override val features = listOf(
        HookFeature(
            key = FEATURE_REAL_BATTERY,
            title = "真实电量（去虚电）",
            description = "拦截 system_server BatteryService 派发 level，使耗电统计首格不再 30~60m 耐用后暴跌，UI 取 FG raw_soc 均匀掉落。",
            defaultEnabled = true
        ),
        HookFeature(
            key = FEATURE_BLOCK_UPDATE,
            title = "屏蔽系统更新",
            description = "开启后将系统更新通道重定向至无效地址以屏蔽 OTA，需 ROOT 权限；关闭即还原。首次开启会检测 ROOT，详见开关内提示。",
            defaultEnabled = false
        )
    )
    private val realBattery = BatteryRealHook()
    override fun install(module: XposedModule, param: PackageLoadedParam) {
        HookLogger.log(LogLevel.INFO, "device", "适配器已注入：${param.packageName}")
        realBattery.install(module, param.defaultClassLoader)
    }

    companion object {
        const val FEATURE_REAL_BATTERY = "lingxi_hook_real_battery"
        const val FEATURE_BLOCK_UPDATE = "lingxi_hook_block_update"
    }
}
