package github.boxiaolanya2008.lingxihook.data

import android.content.Context
import android.provider.Settings

/** 通用设置持久化（应用内部存储 SharedPreferences） */
object AppPrefs {
    private const val FILE = "app_settings"
    private const val KEY_THEME = "theme_mode"

    /** 主题模式（仅浅/深/跟随系统，后端兼容字段） */
    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    // ---- M3 Expressive 主题设置（移植自 KernelSU KernelUI）----
    private const val KEY_COLOR_MODE = "color_mode"      // Int
    private const val KEY_KEY_COLOR = "key_color"        // Int，0 = 动态
    private const val KEY_PALETTE_STYLE = "palette_style" // String
    private const val KEY_COLOR_SPEC = "color_spec"       // String

    /** 动态取色（跟随系统壁纸），等价 ColorMode 0/1/2 */
    const val KEY_COLOR_DYNAMIC = 0

    fun themeMode(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM

    fun setThemeMode(context: Context, mode: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, mode).apply()
    }

    fun colorMode(context: Context): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(KEY_COLOR_MODE, 0)

    fun setColorMode(context: Context, mode: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_COLOR_MODE, mode).apply()
    }

    fun keyColor(context: Context): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(KEY_KEY_COLOR, KEY_COLOR_DYNAMIC)

    fun setKeyColor(context: Context, keyColor: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_KEY_COLOR, keyColor).apply()
    }

    fun paletteStyle(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_PALETTE_STYLE, "TonalSpot") ?: "TonalSpot"

    fun setPaletteStyle(context: Context, style: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_PALETTE_STYLE, style).apply()
    }

    fun colorSpec(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_COLOR_SPEC, "SPEC_2025") ?: "SPEC_2025"

    fun setColorSpec(context: Context, spec: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_COLOR_SPEC, spec).apply()
    }

    /** 导航栏渲染等级：low 关玻璃效果 / mid 默认 / high 拉满（底部导航栏视觉档位） */
    const val NAV_LEVEL_LOW = "low"
    const val NAV_LEVEL_MID = "mid"
    const val NAV_LEVEL_HIGH = "high"
    private const val KEY_NAV_LEVEL = "nav_level"

    fun navLevel(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_NAV_LEVEL, NAV_LEVEL_MID) ?: NAV_LEVEL_MID

    fun setNavLevel(context: Context, level: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_NAV_LEVEL, level).apply()
    }

    /** 是否已授予“修改系统设置”权限（功能开关镜像到 Settings.System 必需） */
    fun canWriteSystemSettings(context: Context): Boolean =
        Settings.System.canWrite(context)

    /**
     * 读取功能开关（仅应用本地记录）。
     * 注意：目标进程实际读取的是 Settings.System 中的镜像值。
     */
    fun isFeatureEnabled(context: Context, key: String, def: Boolean): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(key, def)

    /**
     * 写入功能开关：本地记录 + 镜像到 Settings.System 供目标进程 Hook 读取。
     * 未授予“修改系统设置”权限时镜像失败（静默），本地记录仍生效用于 UI 展示。
     */
    fun setFeatureEnabled(context: Context, key: String, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(key, enabled).apply()
        runCatching {
            Settings.System.putInt(context.contentResolver, key, if (enabled) 1 else 0)
        }
    }
}