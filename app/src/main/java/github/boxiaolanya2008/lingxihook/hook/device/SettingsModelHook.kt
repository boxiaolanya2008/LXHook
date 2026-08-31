package github.boxiaolanya2008.lingxihook.hook.device

import github.boxiaolanya2008.lingxihook.hook.AppHooker
import github.boxiaolanya2008.lingxihook.hook.HookFeature
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

class SettingsModelHook : AppHooker {
    override val packageName = "com.android.settings"
    override val label = "系统设置"
    override val description = "机型伪装"
    override val features = listOf(
        HookFeature("lingxi_hook_device_model", "机型伪装 PD2520→PD2502", "同系统机型伪装，使设置页型号显示为 PD2502。", true)
    )
    private val spoof = ModelSpoofHook()
    override fun install(module: XposedModule, param: PackageLoadedParam) {
        spoof.install(module, param.defaultClassLoader)
    }
}
