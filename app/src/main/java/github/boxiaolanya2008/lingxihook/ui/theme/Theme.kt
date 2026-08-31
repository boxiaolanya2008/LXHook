package github.boxiaolanya2008.lingxihook.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

/** 色彩模式（移植自 KernelSU KernelUI） */
enum class ColorMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2),
    MONET_SYSTEM(3),
    MONET_LIGHT(4),
    MONET_DARK(5),
    DARK_AMOLED(6);

    companion object {
        fun fromValue(value: Int): ColorMode = entries.find { it.value == value } ?: SYSTEM
    }

    val isSystem: Boolean get() = value == 0 || value == 3
    val isDark: Boolean get() = value == 2 || value == 5 || value == 6
    val isAmoled: Boolean get() = value == 6
}

/** 调色板风格（Material You）。Expressive 需要 SPEC_2025 规范 */
val PaletteStyle.supportsSpec2025: Boolean
    get() = this == PaletteStyle.TonalSpot ||
        this == PaletteStyle.Neutral ||
        this == PaletteStyle.Vibrant ||
        this == PaletteStyle.Expressive

fun ColorSpec.SpecVersion.effectiveFor(style: PaletteStyle): ColorSpec.SpecVersion =
    if (this == ColorSpec.SpecVersion.SPEC_2025 && !style.supportsSpec2025) {
        ColorSpec.SpecVersion.SPEC_2021
    } else {
        this
    }

val G2Radius = 16.dp
val G2Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(G2Radius),
    extraLarge = RoundedCornerShape(20.dp)
)

/**
 * 灵犀Hook 主题：M3 Expressive 完整实现（移植自 KernelUI）。
 * - MaterialExpressiveTheme + MotionScheme.expressive()：Expressive 组件体系 + 弹性动效；
 * - materialkolor 动态取色：keyColor == 0 时用系统壁纸主色（Material You），否则用自定义主色；
 * - paletteStyle 可选 Expressive / Vibrant / Neutral / TonalSpot / Rainbow；
 * - 主题切换时整个色板 spring 渐变动画；
 * - 状态栏 / 导航栏图标随明暗自动适配。
 */
@Composable
fun 灵犀HookTheme(
    colorMode: ColorMode = ColorMode.SYSTEM,
    keyColor: Int = 0,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = colorMode.isDark || (colorMode.isSystem && isSystemInDarkTheme())
    val amoled = colorMode.isAmoled

    val colorScheme = rememberLingXiColorScheme(
        seedColor = if (keyColor == 0) Color.Unspecified else Color(keyColor),
        isDark = darkTheme,
        isAmoled = amoled,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
    )

    // 状态栏 / 导航栏图标明暗适配
    LaunchedEffect(darkTheme) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val animatedColorScheme = colorScheme.animateAsState()

    MaterialExpressiveTheme(
        colorScheme = animatedColorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = G2Shapes,
        typography = Typography,
        content = content
    )
}