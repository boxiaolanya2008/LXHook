package github.boxiaolanya2008.lingxihook.hook.device

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.AppHooker
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * 设备型号伪装总入口，覆盖相机与系统设置等。
 * 将 PD2520/V2520A 统一伪装为 PD2502，使 FeatureConfig 加载 PD2502 配置。
 */
class DeviceModelHook : AppHooker {
    override val packageName = "android"
    override val label = "系统机型伪装"
    override val description = "拦截 Build 与 SystemProperties 的型号读取，PD2520/V2520A → PD2502"
    override val features = listOf(
        github.boxiaolanya2008.lingxihook.hook.HookFeature(
            key = "lingxi_hook_device_model",
            title = "机型伪装 PD2520→PD2502",
            description = "Hook Build.MODEL/PRODUCT/DEVICE 与 SystemProperties ro.product.model.bbk 等，V2520A/PD2520 自动替换为 PD2502，使全系统（含相机）识别为 PD2502。",
            defaultEnabled = true
        )
    )
    private val spoof = ModelSpoofHook()
    override fun install(module: XposedModule, param: PackageLoadedParam) {
        HookLogger.log(LogLevel.INFO, "device", "适配器已注入：${param.packageName}")
        spoof.install(module, param.defaultClassLoader)
    }
}
