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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.boxiaolanya2008.lingxihook.ui.pages.AppDetailPage
import github.boxiaolanya2008.lingxihook.ui.pages.HomePage
import github.boxiaolanya2008.lingxihook.ui.pages.LogsPage
import github.boxiaolanya2008.lingxihook.ui.pages.SettingsPage

enum class Page(val label: String) {
    HOME("主页"),
    LOGS("日志"),
    SETTINGS("设置")
}

@Composable
fun AppRoot(
    colorMode: Int,
    keyColor: Int,
    paletteStyle: String,
    onColorModeChange: (Int) -> Unit,
    onKeyColorChange: (Int) -> Unit,
    onPaletteStyleChange: (String) -> Unit
) {
    var page by rememberSaveable { mutableStateOf(Page.HOME) }
    var detailPkg by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = detailPkg != null) { detailPkg = null }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        AnimatedContent(
            targetState = if (detailPkg != null) "app:$detailPkg" else "page:${page.name}",
            transitionSpec = {
                (fadeIn(tween(240)) + slideInHorizontally(tween(240)) { it / 8 })
                    .togetherWith(fadeOut(tween(160)) + slideOutHorizontally(tween(160)) { -it / 10 })
            },
            label = "screen"
        ) { key ->
            val contentModifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = if (detailPkg != null) 24.dp else 100.dp)
            if (key.startsWith("app:")) {
                AppDetailPage(
                    packageName = key.removePrefix("app:"),
                    onBack = { detailPkg = null },
                    modifier = contentModifier
                )
            } else {
                when (page) {
                    Page.HOME -> HomePage(
                        onOpenApp = { pkg -> detailPkg = pkg },
                        modifier = contentModifier
                    )
                    Page.LOGS -> LogsPage(contentModifier)
                    Page.SETTINGS -> SettingsPage(
                        colorMode = colorMode,
                        keyColor = keyColor,
                        paletteStyle = paletteStyle,
                        onColorModeChange = onColorModeChange,
                        onKeyColorChange = onKeyColorChange,
                        onPaletteStyleChange = onPaletteStyleChange,
                        modifier = contentModifier
                    )
                }
            }
        }

        if (detailPkg == null) {
            ShortNavigationBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            ) {
                Page.entries.forEach { p ->
                    val selected = p == page
                    ShortNavigationBarItem(
                        selected = selected,
                        onClick = { page = p },
                        icon = {
                            Text(
                                p.label.take(1),
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        label = { Text(p.label) }
                    )
                }
            }
        }
    }
}
