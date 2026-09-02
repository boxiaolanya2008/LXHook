package github.boxiaolanya2008.lingxihook.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import github.boxiaolanya2008.lingxihook.R
import github.boxiaolanya2008.lingxihook.data.AppPrefs
import github.boxiaolanya2008.lingxihook.ui.component.liquidglass.GlassLevel
import github.boxiaolanya2008.lingxihook.ui.component.liquidglass.LiquidGlassGlobalBarHost
import github.boxiaolanya2008.lingxihook.ui.component.liquidglass.LiquidGlassItem
import github.boxiaolanya2008.lingxihook.ui.pages.AppDetailPage
import github.boxiaolanya2008.lingxihook.ui.pages.HomePage
import github.boxiaolanya2008.lingxihook.ui.pages.LogsPage
import github.boxiaolanya2008.lingxihook.ui.pages.SettingsPage

enum class Page(val label: String, val iconRes: Int) {
    HOME("主页", R.drawable.ic_nav_home),
    LOGS("日志", R.drawable.ic_nav_logs),
    SETTINGS("设置", R.drawable.ic_nav_settings)
}

@Composable
fun AppRoot(
    colorMode: Int,
    keyColor: Int,
    paletteStyle: String,
    navLevel: String,
    onColorModeChange: (Int) -> Unit,
    onKeyColorChange: (Int) -> Unit,
    onPaletteStyleChange: (String) -> Unit,
    onNavLevelChange: (String) -> Unit
) {
    var page by rememberSaveable { mutableStateOf(Page.HOME) }
    var detailPkg by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = detailPkg != null) { detailPkg = null }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        if (detailPkg != null) {
            // 详情页无底部导航栏，全屏展示
            AppDetailPage(
                packageName = detailPkg.orEmpty(),
                onBack = { detailPkg = null },
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = 24.dp)
            )
        } else {
            // 液态玻璃导航栏宿主：内部结构保证 bar 不进入 backdrop 录制层（避免折射 shader 递归崩溃）
            LiquidGlassGlobalBarHost(
                modifier = Modifier.fillMaxSize(),
                items = Page.entries.map { LiquidGlassItem(it.ordinal, it.label, it.iconRes) },
                selectedIndex = { page.ordinal },
                onSelected = { page = Page.entries[it] },
                glassLevel = when (navLevel) {
                    AppPrefs.NAV_LEVEL_LOW -> GlassLevel.LOW
                    AppPrefs.NAV_LEVEL_HIGH -> GlassLevel.HIGH
                    else -> GlassLevel.MID
                }
            ) {
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        (fadeIn(tween(240)) + slideInHorizontally(tween(240)) { it / 8 })
                            .togetherWith(fadeOut(tween(160)) + slideOutHorizontally(tween(160)) { -it / 10 })
                    },
                    label = "page"
                ) { current ->
                    val contentModifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(bottom = 100.dp)
                    when (current) {
                        Page.HOME -> HomePage(
                            onOpenApp = { pkg -> detailPkg = pkg },
                            modifier = contentModifier
                        )
                        Page.LOGS -> LogsPage(contentModifier)
                        Page.SETTINGS -> SettingsPage(
                            colorMode = colorMode,
                            keyColor = keyColor,
                            paletteStyle = paletteStyle,
                            navLevel = navLevel,
                            onColorModeChange = onColorModeChange,
                            onKeyColorChange = onKeyColorChange,
                            onPaletteStyleChange = onPaletteStyleChange,
                            onNavLevelChange = onNavLevelChange,
                            modifier = contentModifier
                        )
                    }
                }
            }
        }
    }
}
