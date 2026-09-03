package github.boxiaolanya2008.lingxihook.ui.component.liquidglass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding

/**
 * App-level liquid-glass global bar host.
 *
 * IMPORTANT layering to avoid render recursion: the LayerBackdrop that records
 * content for the glass bar must capture ONLY the page content, never the bar
 * itself. Structure:
 *   Box (full) {
 *     Box.layerBackdrop(backdrop) { page content }   <- sampled by bar
 *     FloatingBottomBar(...)                          <- sibling, NOT inside backdrop
 *   }
 * If the bar were inside the recorded layer, the refraction shader would sample
 * a frame containing its own surface and recurse until the render stack blows.
 */
@Composable
fun LiquidGlassGlobalBarHost(
    modifier: Modifier = Modifier,
    items: List<LiquidGlassItem>,
    selectedIndex: () -> Int,
    onSelected: (Int) -> Unit,
    glassLevel: GlassLevel = GlassLevel.MID,
    content: @Composable () -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    Box(modifier = modifier) {
        // Only the content participates in the backdrop layer.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            content()
        }

        // The glass bar floats above, sampling `backdrop` (content only).
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            contentAlignment = Alignment.Center,
        ) {
            FloatingBottomBar(
                modifier = Modifier.padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp),
                selectedIndex = selectedIndex,
                onSelected = onSelected,
                backdrop = backdrop,
                tabsCount = items.size,
                glassLevel = glassLevel,
            ) {
                val selectedNow = selectedIndex()
                items.forEach { item ->
                    FloatingBottomBarItem(
                        onClick = { onSelected(items.indexOf(item)) },
                        selected = items.indexOf(item) == selectedNow
                    ) {
                        // 优先用宿主原生 View 提取的图标遮罩位图，否则回退模块向量资源
                        val painter = item.iconBitmap?.let { BitmapPainter(it) }
                            ?: painterResource(item.iconRes)
                        // 色完全交给本栏主题：选中 primary、未选中 onSurfaceVariant，与顶部玻璃栏配色统一
                        val tint = if (items.indexOf(item) == selectedNow) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                        Icon(
                            painter = painter,
                            contentDescription = item.label,
                            tint = tint,
                        )
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
