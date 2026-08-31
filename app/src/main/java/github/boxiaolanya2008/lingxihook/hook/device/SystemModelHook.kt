package github.boxiaolanya2008.lingxihook.hook.device

import github.boxiaolanya2008.lingxihook.hook.AppHooker
import github.boxiaolanya2008.lingxihook.hook.HookFeature
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

class SystemModelHook : AppHooker {
    override val packageName = "system"
    override val label = "系统机型伪装"
    override val description = "同 android 机型伪装，拦截 system 进程型号获取 PD2520/V2520A → PD2502/V2502A"
    override val features = listOf(
        HookFeature("lingxi_hook_device_model", "机型伪装 PD2520→PD2502", "同 android 机型伪装。", true)
    )
    private val spoof = ModelSpoofHook()
    override fun install(module: XposedModule, param: PackageLoadedParam) {
        spoof.install(module, param.defaultClassLoader)
    }
}
