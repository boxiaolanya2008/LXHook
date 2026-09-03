package github.boxiaolanya2008.lingxihook.hook.gamecube

import android.os.SystemClock
import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import java.io.File

/**
 * 超分/插帧配置路径重定向（Hook 点来自 jadx 反编译 com.vivo.gamecube 14.0.15 源码，非猜测）。
 *
 * 链路：`gamedaemon.x#c/d` 经 `j9.a#a(Context, boolean)` 解析
 * `FrameInterConfigList.xml`、`da.a#a(Context, boolean)` 解析
 * `SuperResolutionConfigList.xml`（两入口 `a(Context, boolean)` 本体反编译失败，
 * 但解析器统一吃 `InputStream`，`g9/a.java:22` 证实原路径为
 * `/system/etc/gamecube/` 下的两个同名文件，只读分区无法修改）。
 *
 * 方案：在 `FileInputStream` / `FileReader` 的 `(String)` / `(File)` 构造器上按文件名
 * 精确拦截（`FrameInterConfigList.xml` / `SuperResolutionConfigList.xml`），
 * 仅当游戏自家外部文件目录 `Android/data/<pkg>/files/gamecube/` 下同名文件存在且可读
 * 时才换路，否则照旧读 `/system`（改不生效但绝不崩）。自家目录免存储权限可读，
 * 用 MT 管理器等 all-files 文件管理器即可写入；`g9/a#c` 的 exists 门控因系统文件
 * 一直存在而恒过，无需干预。
 *
 * 热路径注意：游戏进程文件打开频繁，开关用 5 秒 TTL 缓存，避免每次查 Settings；
 * 非目标文件名只做一次 `substringAfterLast` 即放行。
 */
class ConfigPathHook {

    fun install(module: XposedModule, param: PackageLoadedParam) {
        var hooked = 0
        hooked += hookOpen(module, FileInputStreamCtor, arrayOf(String::class.java))
        hooked += hookOpen(module, FileInputStreamCtor, arrayOf(File::class.java))
        hooked += hookOpen(module, FileReaderCtor, arrayOf(String::class.java))
        hooked += hookOpen(module, FileReaderCtor, arrayOf(File::class.java))
        if (hooked == 0) {
            HookLogger.log(LogLevel.WARN, TAG, "no stream ctors hooked")
        } else {
            HookLogger.log(LogLevel.INFO, TAG, "installed ($hooked hooks), override: <app-external-files>/gamecube")
        }
    }

    private fun hookOpen(module: XposedModule, className: String, params: Array<Class<*>>): Int {
        val clazz = runCatching { Class.forName(className) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$className not found: $it")
            return 0
        }
        val ctor = runCatching { clazz.getDeclaredConstructor(*params) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "${clazz.name}(${params.joinToString { it.simpleName }}) not found: $it")
            return 0
        }
        return runCatching {
            module.hook(ctor)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (!isEnabledCached()) return@intercept chain.proceed()
                    val raw = when (val arg = chain.args.getOrNull(0)) {
                        is String -> arg
                        is File -> arg.path
                        else -> return@intercept chain.proceed()
                    }
                    val name = raw.substringAfterLast('/')
                    if (name != FILE_FRAME && name != FILE_SUPER) return@intercept chain.proceed()
                    val override = findOverride(name) ?: return@intercept chain.proceed()
                    chain.args[0] = if (chain.args[0] is File) override else override.absolutePath
                    HookLogger.log(LogLevel.INFO, TAG, "redirect $raw -> ${override.absolutePath}")
                    chain.proceed()
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${clazz.name}(${params.joinToString { it.simpleName }})")
            1
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook ${clazz.name} failed: $it")
        }.getOrDefault(0)
    }

    /** 开关 5 秒 TTL 缓存：FileInputStream 是热路径，不能每次查 Settings */
    private fun isEnabledCached(): Boolean {
        val now = SystemClock.uptimeMillis()
        if (now - cachedAt > CACHE_TTL_MS) {
            cachedOn = HookConfig.isEnabled(VivoGameCubeHook.FEATURE_CONFIG, true)
            cachedAt = now
        }
        return cachedOn
    }

    /**
     * 覆盖文件：游戏自家外部文件目录 `Android/data/<pkg>/files/gamecube/` 下同名文件，
     * 包名运行时取（免硬编码），存在且可读才返回。
     */
    private fun findOverride(name: String): File? = runCatching {
        val app = HookConfig.currentApplication() ?: return null
        val own = File(File(app.getExternalFilesDir(null), DIR_NAME), name)
        if (own.isFile && own.canRead()) own else null
    }.getOrNull()

    private companion object {
        const val TAG = "config"
        const val FileInputStreamCtor = "java.io.FileInputStream"
        const val FileReaderCtor = "java.io.FileReader"
        const val FILE_FRAME = "FrameInterConfigList.xml"
        const val FILE_SUPER = "SuperResolutionConfigList.xml"
        const val DIR_NAME = "gamecube"
        const val CACHE_TTL_MS = 5000L

        @Volatile
        private var cachedOn = true

        @Volatile
        private var cachedAt = 0L
    }
}
