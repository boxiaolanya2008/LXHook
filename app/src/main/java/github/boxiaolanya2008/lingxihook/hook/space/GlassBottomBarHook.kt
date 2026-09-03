package github.boxiaolanya2008.lingxihook.hook.space

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
                            // 单一数据源：ringIndex 同时被「点按」「拖动」「外部同步」驱动，
                            // 保证点击页签后 indicator 也跟随到位
                            selectedIndex = { ringIndex.intValue },
                            onSelected = { index ->
                                ringIndex.intValue = index
                                bridgeClick(menuContainer, index)
                            }
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
            // 等主内容（精选 feed 等）就绪再显示玻璃栏，避免广告/加载页还没结束导航就出现
            composeView.visibility = View.GONE
            revealWhenContentReady(activity, composeView)
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

    /** 收集原导航栏条目的标题与真实图标（图标取自子项内 ImageView 的 drawable，转位图） */
    private fun collectItems(menuContainer: ViewGroup): List<LiquidGlassItem> {
        return (0 until menuContainer.childCount).map { i ->
            val child = menuContainer.getChildAt(i)
            var label = child.contentDescription?.toString().orEmpty()
            if (label.isEmpty()) {
                label = findText(child) ?: "页签${i + 1}"
            }
            val bitmap = findIconBitmap(child)
            LiquidGlassItem(i, label, R.drawable.ic_nav_space_item, bitmap)
        }
    }

    /** 递归提取子项图标遮罩：把单色图标 drawable 画成白色 alpha 遮罩（只留图形形状）。
     *  vivo 底部图标是单色线条图（tint 上色）；这里不烘进任何颜色，颜色交给 Compose Icon
     *  按本栏主题 tint 渲染，保证与官方/本模块配色一致。 */
    private fun findIconBitmap(view: View): ImageBitmap? {
        if (view is ImageView) {
            runCatching {
                val src = view.drawable?.mutate() ?: return@runCatching null
                val w = src.intrinsicWidth.takeIf { it > 0 } ?: 192
                val h = src.intrinsicHeight.takeIf { it > 0 } ?: 192
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                src.setBounds(0, 0, w, h)
                src.draw(canvas)
                // SRC_IN 把已绘制像素统一染成白，保留 alpha 外形
                canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC_IN)
                return bmp.asImageBitmap()
            }.onFailure {
                HookLogger.log(LogLevel.WARN, TAG, "提取图标遮罩失败: $it", persist = false)
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findIconBitmap(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    /** 等主内容就绪且全屏广告浮层消失后再显示玻璃栏；最长 5s 兜底。
     *  广告通常以全屏浮层（logo_adv_layout / dialog_pag_view / popup_container）盖在主页上，
     *  必须等它们消失，导航才不会在第一屏出现。 */
    private fun revealWhenContentReady(activity: Activity, composeView: View) {
        val decor = activity.window.decorView
        val contentId = activity.resources.getIdentifier("fragment_container", "id", TARGET_PACKAGE)
        var revealed = false
        var listener: ViewTreeObserver.OnPreDrawListener? = null
        fun reveal() {
            if (revealed) return
            revealed = true
            composeView.visibility = View.VISIBLE
            listener?.let { decor.viewTreeObserver.removeOnPreDrawListener(it) }
        }
        listener = ViewTreeObserver.OnPreDrawListener {
            if (!revealed) {
                val ready = (contentId == 0 || contentReady(activity.findViewById(contentId))) &&
                    !adOverlayShowing(activity)
                if (ready) reveal()
            }
            true
        }
        decor.viewTreeObserver.addOnPreDrawListener(listener)
        decor.postDelayed({ reveal() }, 5000L)
    }

    private fun contentReady(container: View?): Boolean {
        if (container !is ViewGroup) return false
        for (i in 0 until container.childCount) {
            val c = container.getChildAt(i)
            if (c.visibility == View.VISIBLE && c.height > 0) return true
        }
        return false
    }

    /** 是否有全屏广告浮层当前可见 */
    private fun adOverlayShowing(activity: Activity): Boolean {
        for (name in AD_OVERLAY_IDS) {
            val id = activity.resources.getIdentifier(name, "id", TARGET_PACKAGE)
            if (id != 0) {
                val v = activity.findViewById<View>(id)
                if (v != null && v.visibility == View.VISIBLE && v.width > 0) return true
            }
        }
        return false
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
        /** 全屏广告/浮层 View id（vivospace_main.xml 中）：显示时暂不暴露玻璃栏 */
        val AD_OVERLAY_IDS = listOf("logo_adv_layout", "dialog_pag_view", "popup_container")
    }

    /** 守卫：同一 Activity 只注入一次（onPostCreate 可能因配置变化多次回调） */
    private var injectedActivity: Activity? = null

    /** 玻璃胶囊当前选中索引：Compose state，点按/拖动共用，驱动 indicator 跟随 */
    private val ringIndex = mutableIntStateOf(0)

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
