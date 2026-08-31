package github.boxiaolanya2008.lingxihook.hook.powersaving

import github.boxiaolanya2008.lingxihook.hook.AppHooker
import github.boxiaolanya2008.lingxihook.hook.HookFeature
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import github.boxiaolanya2008.lingxihook.data.LogLevel
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * com.iqoo.powersaving（vivo/iQOO 省电管理）适配入口。
 * 只做分发，各功能独立成类；新增功能时在 install 里追加一行。
 */
class IqooPowerSavingHook : AppHooker {

    override val packageName = "com.iqoo.powersaving"
    override val label = "省电管理（vivo / iQOO）"
    override val description =
        "vivo / iQOO 省电与后台耗电管理系统应用（电池健康与充电设置等）"

    override val features = listOf(
        HookFeature(
            key = FEATURE_WIRELESS,
            title = "无线充电适配",
            description = "强制放行无线充电支持检测（utils.g#E）与摆放位置检测（#F），" +
                "恢复「反向无线充电」「无线充电摆放位置」入口，保证设置界面可正常跳转。",
            defaultEnabled = true
        ),
        HookFeature(
            key = FEATURE_DEEPOPT,
            title = "关闭应用深度优化",
            description = "拦截 appoptimize.b#startDexoptJob/getDexoptPackages/getPredictDexoptTime/getRunningStatus，" +
                "使「应用深度优化」调度直接失败/空列表/已优化态，后台不再触发 dexopt，入口点击无事发生。",
            defaultEnabled = true
        )
    )

    private val wirelessChargeHook = WirelessChargeHook()
    private val deepOptimizationHook = DeepOptimizationHook()

    override fun install(module: XposedModule, param: PackageLoadedParam) {
        HookLogger.log(LogLevel.INFO, "powersaving", "适配器已注入：${param.packageName}")
        wirelessChargeHook.install(module, param)
        deepOptimizationHook.install(module, param)
    }

    companion object {
        /** 无线充电适配的开关持久化键 */
        const val FEATURE_WIRELESS = "lingxi_hook_wireless"
        /** 应用深度优化关闭的开关持久化键 */
        const val FEATURE_DEEPOPT = "lingxi_hook_deepopt"
    }
}