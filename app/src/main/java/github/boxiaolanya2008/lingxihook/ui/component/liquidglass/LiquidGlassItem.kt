package github.boxiaolanya2008.lingxihook.ui.component.liquidglass

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Destination model for the liquid-glass bar.
 * [iconBitmap] 优先（用于 vivo 社区等宿主原生 View 提取的真实图标位图），
 * 为 null 时回退用 [iconRes] 指向的向量资源（Material Symbols XML in res/drawable）。
 */
data class LiquidGlassItem(
    val id: Int,
    val label: String,
    val iconRes: Int,
    val iconBitmap: ImageBitmap? = null,
)
