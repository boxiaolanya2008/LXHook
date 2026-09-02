package github.boxiaolanya2008.lingxihook.hook

import github.boxiaolanya2008.lingxihook.hook.abe.VivoAbeHook
import github.boxiaolanya2008.lingxihook.hook.camera.VivoCameraHook
import github.boxiaolanya2008.lingxihook.hook.fuelsummary.VivoFuelSummaryHook
import github.boxiaolanya2008.lingxihook.hook.space.VivoSpaceHook
import github.boxiaolanya2008.lingxihook.hook.device.DeviceModelHook
import github.boxiaolanya2008.lingxihook.hook.device.SettingsModelHook
import github.boxiaolanya2008.lingxihook.hook.device.SystemModelHook
import github.boxiaolanya2008.lingxihook.hook.powersaving.IqooPowerSavingHook

/**
 * 适配注册表：新增应用适配时在这里加一行即可。
 * LingXiHook 按包名分发；主界面适配列表也从这里渲染，无需改 UI。
 */
object HookRegistry {

    val all: List<AppHooker> = listOf(
        IqooPowerSavingHook(),
        VivoCameraHook(),
        DeviceModelHook(),
        SettingsModelHook(),
        SystemModelHook(),
        VivoAbeHook(),
        VivoFuelSummaryHook(),
        VivoSpaceHook()
    )

    fun find(packageName: String): AppHooker? = all.firstOrNull { it.packageName == packageName }
}