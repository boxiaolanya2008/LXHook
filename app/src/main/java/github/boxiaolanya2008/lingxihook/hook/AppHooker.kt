package github.boxiaolanya2008.lingxihook.hook

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/** 一个可独立开关的 Hook 功能（UI 展示 + 开关持久化的最小单元） */
data class HookFeature(
    /** 持久化键（AppPrefs + Settings.System 同名镜像） */
    val key: String,
    /** 展示标题 */
    val title: String,
    /** 功能说明（适配详情页展示） */
    val description: String,
    /** 默认是否开启 */
    val defaultEnabled: Boolean = true
)

/**
 * 一个“目标应用适配器”= 一个被 Hook 的应用。
 * 新增适配：写一个实现类 + 在 [HookRegistry] 注册，分发与主界面列表自动生效。
 */
interface AppHooker {
    /** 目标应用包名，必须与 scope.list / arrays.xml 中声明一致 */
    val packageName: String

    /** 展示用名称 */
    val label: String

    /** 应用说明（适配详情页展示） */
    val description: String

    /** 可开关的功能清单 */
    val features: List<HookFeature>

    /** 目标应用进程加载时执行 Hook 安装 */
    fun install(module: XposedModule, param: PackageLoadedParam)
}