package github.boxiaolanya2008.lingxihook.hook.gamecube

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.AppHooker
import github.boxiaolanya2008.lingxihook.hook.HookFeature
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * com.vivo.gamecube（游戏魔盒 / 游戏伴侣）适配入口。
 * 只做分发，各功能独立成类；新增功能时在 install 里追加一行。
 */
class VivoGameCubeHook : AppHooker {

    override val packageName = "com.vivo.gamecube"
    override val label = "游戏魔盒（vivo gamecube）"
    override val description =
        "vivo 游戏魔盒，含超分/插帧游戏名单配置（/system/etc/gamecube/ 只读）"

    override val features = listOf(
        HookFeature(
            key = FEATURE_CONFIG,
            title = "配置路径重定向",
            description = "拦截 FrameInterConfigList.xml / SuperResolutionConfigList.xml 的文件打开，" +
                "游戏自家 Android/data 目录 files/gamecube/ 下有同名可读文件即换路，" +
                "没有则照旧读 /system，绝不影响游戏启动。",
            defaultEnabled = true
        ),
        HookFeature(
            key = FEATURE_QZONE,
            title = "Q芯片专区入口强制开启",
            description = "拦截功能白名单总闸 e0#h0，对 boost_frame*/optimize_power*/super_resolution/game_super_hd_engine " +
                "强制 true，补齐 PD2520 不在机型维度白名单的缺口，使侧滑 Q 芯片专区的“分辨率与帧率”" +
                "（超分 + 极致帧率）入口不再按机型阉割。",
            defaultEnabled = true
        ),
        HookFeature(
            key = FEATURE_DISPLAY,
            title = "显示设置强制开启",
            description = "拦截功能白名单总闸 e0#h0，对 display_settings 强制 true，" +
                "放开魔盒内“显示设置”（硬件维度白名单仅到 PD2254 等旧机型）。",
            defaultEnabled = true
        ),
        HookFeature(
            key = FEATURE_LIGHT_TRACK,
            title = "游戏光追强制开启",
            description = "拦截 m0#f（Settings.Global xpq_whitelist_apps 光追白名单判定）强制 true，" +
                "任意游戏放出 Q 区“光影追踪”条目；底层 XPQ 服务效果需硬件支持，以实测为准。",
            defaultEnabled = true
        ),
        HookFeature(
            key = FEATURE_TAA,
            title = "TAA / 抗锯齿强制开启",
            description = "拦截 e0#F0/t0/B0 三处硬编码游戏名单（原神/星铁/绝区零/鸣潮）与 q3.0 引擎判定，" +
                "强制 true 使任意游戏放出 TAA 与抗锯齿选项。",
            defaultEnabled = true
        )
    )

    private val configPathHook = ConfigPathHook()
    private val qZoneGateHook = QZoneGateHook()
    private val displaySettingsHook = DisplaySettingsHook()
    private val lightTrackHook = LightTrackHook()
    private val taaHook = TaaHook()

    override fun install(module: XposedModule, param: PackageLoadedParam) {
        HookLogger.log(LogLevel.INFO, "gamecube", "适配器已注入：${param.packageName}")
        configPathHook.install(module, param)
        qZoneGateHook.install(module, param)
        displaySettingsHook.install(module, param)
        lightTrackHook.install(module, param)
        taaHook.install(module, param)
    }

    companion object {
        /** 配置路径重定向开关持久化键 */
        const val FEATURE_CONFIG = "lingxi_hook_gamecube_config"
        /** Q 芯片专区入口强制开启开关持久化键 */
        const val FEATURE_QZONE = "lingxi_hook_gamecube_qzone"
        /** 显示设置强制开启开关持久化键 */
        const val FEATURE_DISPLAY = "lingxi_hook_gamecube_display"
        /** 游戏光追强制开启开关持久化键 */
        const val FEATURE_LIGHT_TRACK = "lingxi_hook_gamecube_lighttrack"
        /** TAA/抗锯齿强制开启开关持久化键 */
        const val FEATURE_TAA = "lingxi_hook_gamecube_taa"
    }
}
