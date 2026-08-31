package github.boxiaolanya2008.lingxihook.hook

import github.boxiaolanya2008.lingxihook.hook.powersaving.IqooPowerSavingHook

/**
 * 适配注册表：新增应用适配时在这里加一行即可。
 * LingXiHook 按包名分发；主界面适配列表也从这里渲染，无需改 UI。
 */
object HookRegistry {

    val all: List<AppHooker> = listOf(
        IqooPowerSavingHook()
    )

    fun find(packageName: String): AppHooker? = all.firstOrNull { it.packageName == packageName }
}