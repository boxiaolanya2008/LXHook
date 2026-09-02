package github.boxiaolanya2008.lingxihook.hook.space

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import github.boxiaolanya2008.lingxihook.R
import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import github.boxiaolanya2008.lingxihook.ui.component.liquidglass.LiquidGlassGlobalBarHost
import github.boxiaolanya2008.lingxihook.ui.component.liquidglass.LiquidGlassItem
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * vivo 社区底部导航栏替换为本模块液态玻璃导航 Hook。
 *
 * Hook 点（jadx 反编译 com.vivo.space 确认，非猜测）：
 * - com.vivo.space.ui.VivoSpaceTabActivity#onPostCreate：布局已 inflate 完，
 *   R.id.v_bottom_view (SpaceVBottomNavigationView) 已由 onCreate findViewById 拿到。
 * - VBottomNavigationView 内部有 VMenuViewLayout 菜单容器，子项 y2.b 即 tab 条目，
 *   对其 performClick 可桥接原页面切换。
 *
 * 方案：把本模块 dex 里的 ComposeView（含 miuix-blur 液态玻璃导航）挂到
 * Activity 的 android.R.id.content 底部，原导航栏 GONE 隐藏；玻璃胶囊点按/拖动
 * 换页时回调 performClick 桥接原条目。注意 Compose 无法采样宿主原生 View 画面，
 * 折射层背景由 LiquidGlassGlobalBarHost 的 surfaceColor 兜底。注入失败时兜底走
 * 原生玻璃化样式，功能不回退。
 */
class GlassBottomBarHook {

    /** onPostCreate 声明在父类 AppBaseActivity，需沿继承链向上查找 */
    private fun findOnPostCreate(activityClass: Class<*>): java.lang.reflect.Method? {
        var clazz: Class<*>? = activityClass
        while (clazz != null && clazz != Any::class.java) {
            runCatching { return clazz.getDeclaredMethod("onPostCreate", Bundle::class.java) }
            clazz = clazz.superclass
        }
        return null
    }

    fun install(module: XposedModule, param: PackageLoadedParam) {
        val activityClass = runCatching {
            Class.forName(CLASS_TAB_ACTIVITY, false, param.defaultClassLoader)
        }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_TAB_ACTIVITY not found: $it")
            return
        }
        val onPostCreate = findOnPostCreate(activityClass) ?: run {
            HookLogger.log(LogLevel.WARN, TAG, "onPostCreate not found in hierarchy")
            return
        }
        runCatching {
            module.hook(onPostCreate)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val activity = chain.thisObject as? Activity
                    chain.proceed()
                    if (activity != null && activity != injectedActivity &&
                        HookConfig.isEnabled(VivoSpaceHook.FEATURE_GLASS_NAV, true)
                    ) {
                        injectedActivity = activity
                        activity.runOnUiThread { replaceWithGlassBar(activity) }
                    }
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${onPostCreate.declaringClass.name}#onPostCreate")
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook onPostCreate failed: $it")
        }
    }

    /** 主流程：找到原导航栏 → 注入 Compose 玻璃导航 → 隐藏原栏；失败走原生样式兜底 */
    private fun replaceWithGlassBar(activity: Activity) {
        val bar = findBottomBar(activity) ?: run {
            HookLogger.log(LogLevel.WARN, TAG, "v_bottom_view not found, skip")
            return
        }
        val menuContainer = findMenuContainer(bar)
        if (menuContainer == null || menuContainer.childCount == 0) {
            HookLogger.log(LogLevel.WARN, TAG, "menu container not found, fallback to native style")
            applyNativeGlassStyle(bar)
            return
        }
        val items = collectItems(menuContainer)
        runCatching {
            val moduleContext = activity.createPackageContext(
                MODULE_PACKAGE,
                Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
            )
            val composeView = ComposeView(activity)
            composeView.setContent {
                CompositionLocalProvider(LocalContext provides moduleContext) {
                    val isDark = (activity.resources.configuration.uiMode and
                        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                    MaterialTheme(
                        colorScheme = if (isDark) darkColorScheme() else lightColorScheme()
                    ) {
                        LiquidGlassGlobalBarHost(
                            items = items,
                            selectedIndex = { 0 },
                            onSelected = { index -> bridgeClick(menuContainer, index) }
                        ) {
                            // 占位：宿主页面内容由 vivo 社区自己的 View 树渲染
                            androidx.compose.foundation.layout.Spacer(
                                androidx.compose.ui.Modifier
                            )
                        }
                    }
                }
            }
            composeView.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
            // 宿主 androidx 与本模块 androidx 是两套类加载器，宿主 decorView 上的
            // ViewTreeLifecycleOwner tag id 对本模块不可见；自配 owner 供 Recomposer 使用
            val owner = GlassLifecycleOwner()
            owner.start()
            composeView.setViewTreeLifecycleOwner(owner)
            composeView.setViewTreeSavedStateRegistryOwner(owner)
            val content = activity.findViewById<FrameLayout>(android.R.id.content)
            content.addView(composeView)
            bar.visibility = View.GONE
            HookLogger.log(LogLevel.INFO, TAG, "compose glass bar injected (${items.size} tabs)")
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "compose inject failed, fallback native: $it")
            applyNativeGlassStyle(bar)
        }
    }

    /** 桥接：把玻璃胶囊的页签选择转发给原导航栏条目的 performClick */
    private fun bridgeClick(menuContainer: ViewGroup, index: Int) {
        if (index < 0 || index >= menuContainer.childCount) return
        runCatching {
            menuContainer.getChildAt(index).performClick()
            HookLogger.log(LogLevel.INFO, TAG, "bridged tab click -> $index", persist = false)
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "bridge click failed: $it")
        }
    }

    /** 收集原导航栏条目的标题（contentDescription 或子 TextView 文本），供玻璃胶囊显示 */
    private fun collectItems(menuContainer: ViewGroup): List<LiquidGlassItem> {
        return (0 until menuContainer.childCount).map { i ->
            val child = menuContainer.getChildAt(i)
            var label = child.contentDescription?.toString().orEmpty()
            if (label.isEmpty()) {
                label = findText(child) ?: "页签${i + 1}"
            }
            LiquidGlassItem(i, label, R.drawable.ic_nav_space_item)
        }
    }

    private fun findText(view: View): String? {
        if (view is TextView) return view.text?.toString()?.takeIf { it.isNotBlank() }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findText(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    /** 递归找 VBottomNavigationView 内部的 VMenuViewLayout 菜单容器 */
    private fun findMenuContainer(view: View): ViewGroup? {
        if (view is ViewGroup) {
            if (view.javaClass.simpleName == MENU_CONTAINER) return view
            for (i in 0 until view.childCount) {
                findMenuContainer(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun findBottomBar(activity: Activity): View? {
        val id = activity.resources.getIdentifier("v_bottom_view", "id", TARGET_PACKAGE)
        if (id == 0) return null
        return activity.findViewById(id)
    }

    /** 兜底：注入失败时对原导航栏就地玻璃化（悬浮胶囊样式） */
    private fun applyNativeGlassStyle(view: View) {
        if (!HookConfig.isEnabled(VivoSpaceHook.FEATURE_GLASS_NAV, true)) return
        runCatching {
            val density = view.resources.displayMetrics.density
            fun dp(v: Int): Int = (v * density).toInt()
            val isDark = (view.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val glass = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(30).toFloat()
                setColor(if (isDark) 0xD91D1F23.toInt() else 0xCCFFFFFF.toInt())
            }
            view.background = glass
            view.setPadding(dp(12), 0, dp(12), 0)
            view.elevation = dp(12).toFloat()
            view.clipToOutline = true
            val lp = view.layoutParams as? ViewGroup.MarginLayoutParams
            if (lp != null) {
                lp.leftMargin = dp(16)
                lp.rightMargin = dp(16)
                lp.bottomMargin = dp(12)
                view.layoutParams = lp
            }
            HookLogger.log(LogLevel.INFO, TAG, "native glass fallback applied")
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "apply native glass failed: $it")
        }
    }

    private companion object {
        const val TAG = "space"
        const val CLASS_TAB_ACTIVITY = "com.vivo.space.ui.VivoSpaceTabActivity"
        const val MENU_CONTAINER = "VMenuViewLayout"
        const val TARGET_PACKAGE = "com.vivo.space"
        const val MODULE_PACKAGE = "github.boxiaolanya2008.lingxihook"
    }

    /** 守卫：同一 Activity 只注入一次（onPostCreate 可能因配置变化多次回调） */
    private var injectedActivity: Activity? = null

    /**
     * 注入用生命周期所有者：模块类加载器内自建，不依赖宿主 androidx。
     * start 后常驻 RESUMED；宿主 Activity 销毁时 Recomposer 随进程回收（注释性泄漏可接受）。
     */
    private class GlassLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry get() = savedStateController.savedStateRegistry

        fun start() {
            savedStateController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }
}
