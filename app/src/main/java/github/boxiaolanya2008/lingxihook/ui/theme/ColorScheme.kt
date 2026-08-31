package github.boxiaolanya2008.lingxihook.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

@Composable
fun rememberLingXiColorScheme(
    seedColor: Color,
    isDark: Boolean,
    isAmoled: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
): ColorScheme {
    val context = LocalContext.current
    val effectiveSpec = colorSpec.effectiveFor(paletteStyle)
    val effectiveSeed = if (seedColor == Color.Unspecified) {
        // 修复：动态取色也让 paletteStyle 生效——取系统 Monet 主色作 seed 再按所选 style 重算
        val sys = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        sys.primary
    } else seedColor
    val scheme = rememberDynamicColorScheme(
        seedColor = effectiveSeed,
        isDark = isDark,
        isAmoled = isAmoled,
        style = paletteStyle,
        specVersion = effectiveSpec
    )
    return if (isAmoled && isDark) {
        scheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerHigh = Color(0xFF121212),
            surfaceContainerHighest = Color(0xFF1A1A1A)
        )
    } else scheme
}

@Composable
fun ColorScheme.animateAsState(): ColorScheme {
    val springSpec = spring<Color>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    val primary by animateColorAsState(primary, springSpec, label = "primary")
    val onPrimary by animateColorAsState(onPrimary, springSpec, label = "onPrimary")
    val primaryContainer by animateColorAsState(primaryContainer, springSpec, label = "primaryContainer")
    val onPrimaryContainer by animateColorAsState(onPrimaryContainer, springSpec, label = "onPrimaryContainer")
    val secondary by animateColorAsState(secondary, springSpec, label = "secondary")
    val onSecondary by animateColorAsState(onSecondary, springSpec, label = "onSecondary")
    val secondaryContainer by animateColorAsState(secondaryContainer, springSpec, label = "secondaryContainer")
    val onSecondaryContainer by animateColorAsState(onSecondaryContainer, springSpec, label = "onSecondaryContainer")
    val tertiary by animateColorAsState(tertiary, springSpec, label = "tertiary")
    val onTertiary by animateColorAsState(onTertiary, springSpec, label = "onTertiary")
    val tertiaryContainer by animateColorAsState(tertiaryContainer, springSpec, label = "tertiaryContainer")
    val onTertiaryContainer by animateColorAsState(onTertiaryContainer, springSpec, label = "onTertiaryContainer")
    val error by animateColorAsState(error, springSpec, label = "error")
    val onError by animateColorAsState(onError, springSpec, label = "onError")
    val errorContainer by animateColorAsState(errorContainer, springSpec, label = "errorContainer")
    val onErrorContainer by animateColorAsState(onErrorContainer, springSpec, label = "onErrorContainer")
    val background by animateColorAsState(background, springSpec, label = "background")
    val onBackground by animateColorAsState(onBackground, springSpec, label = "onBackground")
    val surface by animateColorAsState(surface, springSpec, label = "surface")
    val onSurface by animateColorAsState(onSurface, springSpec, label = "onSurface")
    val surfaceVariant by animateColorAsState(surfaceVariant, springSpec, label = "surfaceVariant")
    val onSurfaceVariant by animateColorAsState(onSurfaceVariant, springSpec, label = "onSurfaceVariant")
    val outline by animateColorAsState(outline, springSpec, label = "outline")
    val outlineVariant by animateColorAsState(outlineVariant, springSpec, label = "outlineVariant")
    val scrim by animateColorAsState(scrim, springSpec, label = "scrim")
    val inverseSurface by animateColorAsState(inverseSurface, springSpec, label = "inverseSurface")
    val inverseOnSurface by animateColorAsState(inverseOnSurface, springSpec, label = "inverseOnSurface")
    val inversePrimary by animateColorAsState(inversePrimary, springSpec, label = "inversePrimary")
    val surfaceDim by animateColorAsState(surfaceDim, springSpec, label = "surfaceDim")
    val surfaceBright by animateColorAsState(surfaceBright, springSpec, label = "surfaceBright")
    val surfaceContainer by animateColorAsState(surfaceContainer, springSpec, label = "surfaceContainer")
    val surfaceContainerHigh by animateColorAsState(surfaceContainerHigh, springSpec, label = "surfaceContainerHigh")
    val surfaceContainerHighest by animateColorAsState(surfaceContainerHighest, springSpec, label = "surfaceContainerHighest")
    val surfaceContainerLow by animateColorAsState(surfaceContainerLow, springSpec, label = "surfaceContainerLow")
    val surfaceContainerLowest by animateColorAsState(surfaceContainerLowest, springSpec, label = "surfaceContainerLowest")

    return copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        inversePrimary = inversePrimary,
        surfaceDim = surfaceDim,
        surfaceBright = surfaceBright,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = surfaceContainerLowest
    )
}
