package github.boxiaolanya2008.lingxihook.hook.space

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.AppHooker
import github.boxiaolanya2008.lingxihook.hook.HookFeature
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * com.vivo.space（vivo 官方社区）适配入口。
 * 只做分发，各功能独立成类；新增功能时在 install 里追加一行。
 */
class VivoSpaceHook : AppHooker {

    override val packageName = "com.vivo.space"
    override val label = "vivo 社区（com.vivo.space）"
    override val description =
        "vivo 官方社区应用，主界面为 vivospace_main.xml + SpaceVBottomNavigationView 底部导航"

    override val features = listOf(
        HookFeature(
            key = FEATURE_GLASS_NAV,
            title = "液态玻璃导航栏（替换）",
            description = "拦截 VivoSpaceTabActivity#onPostCreate，把本模块的 ComposeView 液态玻璃导航栏" +
                "（含流光/粒子/拖动回弹）挂到窗口底部并隐藏原 SpaceVBottomNavigationView，" +
                "玻璃胶囊换页时 performClick 桥接原条目；注入失败自动兜底为原生玻璃化样式。",
            defaultEnabled = true
        ),
        HookFeature(
            key = FEATURE_ADBLOCK,
            title = "广告屏蔽",
            description = "屏蔽开屏/全屏浮层广告（logo_adv_layout/dialog_pag_view/popup_container）" +
                "与首页推荐流广告横条（viewType 0/23/28/33/34），低频轮询防广告反弹。",
            defaultEnabled = true
        )
    )

    private val glassBottomBarHook = GlassBottomBarHook()
    private val adBlockerHook = AdBlockerHook()

    override fun install(module: XposedModule, param: PackageLoadedParam) {
        HookLogger.log(LogLevel.INFO, "space", "适配器已注入：${param.packageName}")
        glassBottomBarHook.install(module, param)
        adBlockerHook.install(module, param)
    }

    companion object {
        /** 液态玻璃导航栏开关持久化键 */
        const val FEATURE_GLASS_NAV = "lingxi_hook_space_glass_nav"
        /** 广告屏蔽开关持久化键 */
        const val FEATURE_ADBLOCK = "lingxi_hook_space_adblock"
    }
}
