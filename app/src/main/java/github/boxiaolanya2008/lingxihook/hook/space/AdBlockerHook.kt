package github.boxiaolanya2008.lingxihook.hook.space

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * vivo 社区广告屏蔽 Hook（jadx 反编译 com.vivo.space 确认类名/viewType，非猜测）。
 */
class AdBlockerHook {

    fun install(module: XposedModule, param: PackageLoadedParam) {
        hookSplash(module, param)
        hookHomeCrossBanner(module, param)
    }

    private fun hookSplash(module: XposedModule, param: PackageLoadedParam) {
        val activityClass = runCatching {
            Class.forName(CLASS_TAB_ACTIVITY, false, param.defaultClassLoader)
        }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_TAB_ACTIVITY not found for adblock: $it")
            return
        }
        val onPostCreate = findOnPostCreate(activityClass) ?: return
        runCatching {
            module.hook(onPostCreate)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val activity = chain.thisObject as? Activity
                    chain.proceed()
                    if (activity != null && HookConfig.isEnabled(VivoSpaceHook.FEATURE_ADBLOCK, true)) {
                        activity.runOnUiThread { startSplashGuard(activity) }
                    }
                }
            HookLogger.log(LogLevel.INFO, TAG, "adblock: hooked splash onPostCreate")
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "adblock: hook splash failed: $it")
        }
    }

    private fun startSplashGuard(activity: Activity) {
        if (splashGuardStarted) return
        splashGuardStarted = true
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                hideSplashAds(activity)
                handler.postDelayed(this, AD_POLL_MS)
            }
        }
        handler.post(runnable)
    }

    private fun hideSplashAds(activity: Activity) {
        for (name in SPLASH_ID_NAMES) {
            val id = activity.resources.getIdentifier(name, "id", TARGET_PACKAGE)
            if (id != 0) {
                runCatching {
                    val v = activity.findViewById<View>(id)
                    if (v != null && v.visibility != View.GONE) {
                        v.visibility = View.GONE
                        HookLogger.log(LogLevel.INFO, TAG, "adblock: hide splash $name", persist = false)
                    }
                }
            }
        }
        runCatching {
            val decor = activity.window.decorView as? ViewGroup ?: return
            val screenW = decor.width
            if (screenW > 0) {
                for (i in 0 until decor.childCount) {
                    val child = decor.getChildAt(i) ?: continue
                    if (child.width == screenW && child.height >= decor.height - 200) {
                        child.visibility = View.GONE
                        HookLogger.log(
                            LogLevel.INFO, TAG,
                            "adblock: hide fullscreen overlay (${child.javaClass.simpleName})", persist = false
                        )
                    }
                }
            }
        }
    }

    private fun hookHomeCrossBanner(module: XposedModule, param: PackageLoadedParam) {
        val adapterClass = runCatching {
            Class.forName(CLASS_RECOMMEND_ADAPTER, false, param.defaultClassLoader)
        }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_RECOMMEND_ADAPTER not found for adblock: $it")
            return
        }
        val onCreate = adapterClass.declaredMethods.firstOrNull {
            it.name == METHOD_ON_CREATE && it.parameterTypes.size == 2 &&
                it.parameterTypes[1] == Int::class.javaPrimitiveType
        } ?: run {
            HookLogger.log(LogLevel.WARN, TAG, "adblock: onCreateViewHolder not found in ${adapterClass.name}")
            return
        }
        val holderClass = onCreate.returnType
        val holderCtor = runCatching { holderClass.getConstructor(View::class.java) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "adblock: ${holderClass.name}(View) ctor not found: $it")
            return
        }
        runCatching {
            module.hook(onCreate)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val viewType = chain.args[1] as Int
                    if (viewType in AD_VIEW_TYPES) {
                        val fake = createEmptyHolder(chain.args[0] as ViewGroup, holderCtor)
                        HookLogger.log(LogLevel.INFO, TAG, "adblock: suppress feed viewType=$viewType", persist = false)
                        return@intercept fake
                    }
                    chain.proceed()
                }
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "adblock: hook onCreateViewHolder failed: $it")
        }

        val onBind = adapterClass.declaredMethods.firstOrNull {
            it.name == METHOD_ON_BIND && it.parameterTypes.size == 2 &&
                it.parameterTypes[1] == Int::class.javaPrimitiveType
        }
        if (onBind == null) {
            HookLogger.log(LogLevel.WARN, TAG, "adblock: onBindViewHolder not found, skip bind hook")
        } else runCatching {
            module.hook(onBind)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val viewHolder = chain.args[0]
                    val position = chain.args[1] as Int
                    val adapter = chain.thisObject
                    if (isAdapterPositionAd(adapter, position)) {
                        runCatching {
                            val itemView = viewHolder.javaClass.superclass
                                .getDeclaredField("itemView").apply { isAccessible = true }
                                .get(viewHolder) as? View
                            itemView?.visibility = View.GONE
                            itemView?.layoutParams = itemView?.layoutParams?.apply { height = 0 }
                        }
                        HookLogger.log(LogLevel.INFO, TAG, "adblock: skip bind position=$position", persist = false)
                        return@intercept Unit
                    }
                    chain.proceed()
                }
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "adblock: hook onBindViewHolder skipped: $it")
        }
        HookLogger.log(LogLevel.INFO, TAG, "adblock: hooked home feed ads")
    }

    private val AD_VIEW_TYPES = setOf(0, 28, 23, 33, 34)

    private fun createEmptyHolder(parent: ViewGroup, ctor: java.lang.reflect.Constructor<*>): Any {
        val itemView = View(parent.context).apply {
            visibility = View.GONE
            layoutParams = ViewGroup.LayoutParams(0, 0)
        }
        return ctor.newInstance(itemView)
    }

    private fun isAdapterPositionAd(adapter: Any, position: Int): Boolean {
        return runCatching {
            val getItemType = adapter.javaClass.getMethod("getItemViewType", Int::class.javaPrimitiveType)
            val vt = getItemType.invoke(adapter, position) as? Int
            vt in AD_VIEW_TYPES
        }.getOrDefault(false)
    }

    private fun findOnPostCreate(clazz: Class<*>): java.lang.reflect.Method? {
        var c: Class<*>? = clazz
        while (c != null) {
            runCatching {
                val m = c.getDeclaredMethod("onPostCreate", Bundle::class.java)
                m.isAccessible = true
                return m
            }
            c = c.superclass
        }
        return null
    }

    private companion object {
        const val TAG = "space.adblock"
        const val TARGET_PACKAGE = "com.vivo.space"
        const val CLASS_TAB_ACTIVITY = "com.vivo.space.ui.VivoSpaceTabActivity"
        const val CLASS_RECOMMEND_ADAPTER = "com.vivo.space.adapter.RecommendPageRecyclerAdapter"
        const val METHOD_ON_CREATE = "onCreateViewHolder"
        const val METHOD_ON_BIND = "onBindViewHolder"

        val SPLASH_ID_NAMES = listOf("logo_adv_layout", "dialog_pag_view", "popup_container")
        const val AD_POLL_MS = 800L

        @Volatile
        private var splashGuardStarted = false
    }
}
