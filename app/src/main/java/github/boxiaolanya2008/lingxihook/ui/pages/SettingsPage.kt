package github.boxiaolanya2008.lingxihook.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.boxiaolanya2008.lingxihook.ui.component.SegmentedColumn
import github.boxiaolanya2008.lingxihook.ui.component.SegmentedListItem
import github.boxiaolanya2008.lingxihook.ui.theme.ColorMode
import github.boxiaolanya2008.lingxihook.ui.theme.keyColorOptions

private val colorModeLabels: List<Pair<ColorMode, String>> = listOf(
    ColorMode.SYSTEM to "跟随系统",
    ColorMode.LIGHT to "浅色",
    ColorMode.DARK to "深色",
    ColorMode.MONET_SYSTEM to "动态·系统",
    ColorMode.MONET_LIGHT to "动态·浅色",
    ColorMode.MONET_DARK to "动态·深色",
    ColorMode.DARK_AMOLED to "OLED 深色"
)

private val paletteStyleOptions: List<Pair<String, String>> = listOf(
    "TonalSpot" to "Tonal Spot",
    "Vibrant" to "Vibrant",
    "Expressive" to "Expressive",
    "Neutral" to "Neutral",
    "Rainbow" to "Rainbow"
)

@Composable
fun SettingsPage(
    colorMode: Int,
    keyColor: Int,
    paletteStyle: String,
    onColorModeChange: (Int) -> Unit,
    onKeyColorChange: (Int) -> Unit,
    onPaletteStyleChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        ThemeSettingsColumn(
            colorMode = colorMode,
            keyColor = keyColor,
            paletteStyle = paletteStyle,
            onColorModeChange = onColorModeChange,
            onKeyColorChange = onKeyColorChange,
            onPaletteStyleChange = onPaletteStyleChange
        )
        LogSettingCard()
        AboutCard()
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ThemeSettingsColumn(
    colorMode: Int,
    keyColor: Int,
    paletteStyle: String,
    onColorModeChange: (Int) -> Unit,
    onKeyColorChange: (Int) -> Unit,
    onPaletteStyleChange: (String) -> Unit
) {
    SegmentedColumn(
        title = "主题设置",
        modifier = Modifier.fillMaxWidth(),
        content = listOf(
            {
                SegmentedListItem(
                    headlineContent = { Text("色彩模式", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = CardDarkText) },
                    supportingContent = {
                        Column {
                            Text(
                                "跟随系统 / 固定浅深 / 动态取色（MONET）",
                                fontSize = 11.sp,
                                color = CardGrayText
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                colorModeLabels.forEach { (mode, label) ->
                                    FilterChip(
                                        selected = colorMode == mode.value,
                                        onClick = { onColorModeChange(mode.value) },
                                        label = { Text(label, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }
                )
            },
            {
                SegmentedListItem(
                    headlineContent = { Text("自定义主色", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = CardDarkText) },
                    supportingContent = {
                        Column {
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ColorDot(
                                    color = null,
                                    selected = keyColor == 0,
                                    onClick = { onKeyColorChange(0) }
                                )
                                keyColorOptions.forEach { argb ->
                                    ColorDot(
                                        color = Color(argb),
                                        selected = keyColor == argb,
                                        onClick = { onKeyColorChange(argb) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("「◎」= 跟随系统壁纸动态取色；点色块用自定义主色。", fontSize = 11.sp, color = CardGrayText)
                        }
                    }
                )
            },
            {
                SegmentedListItem(
                    headlineContent = { Text("调色板风格", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = CardDarkText) },
                    supportingContent = {
                        Column {
                            Box(Modifier.horizontalScroll(rememberScrollState())) {
                                SingleChoiceSegmentedButtonRow {
                                    paletteStyleOptions.forEachIndexed { index, (style, label) ->
                                        val selected = paletteStyle == style
                                        SegmentedButton(
                                            shape = SegmentedButtonDefaults.itemShape(index = index, count = paletteStyleOptions.size),
                                            selected = selected,
                                            onClick = { onPaletteStyleChange(style) },
                                            icon = {
                                                SegmentedButtonDefaults.Icon(active = selected) {
                                                    Text("✓", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        ) {
                                            Text(label, fontSize = 12.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Expressive 使用 2025 色彩规范，均呈 M3 Expressive 形态；不支持 2025 规范的风格会自动降级到经典规范。",
                                fontSize = 11.sp, lineHeight = 16.sp, color = CardGrayText
                            )
                        }
                    }
                )
            }
        )
    )
}

@Composable
private fun ColorDot(
    color: Color?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val size = 34.dp
    val fill = if (color == null) MaterialTheme.colorScheme.primaryContainer else color
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (color == null) {
            Text("◎", fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun LogSettingCard() {
    SegmentedColumn(
        title = "日志",
        modifier = Modifier.fillMaxWidth(),
        content = listOf(
            {
                SegmentedListItem(
                    headlineContent = { Text("存储位置", fontSize = 14.sp) },
                    supportingContent = {
                        Text(
                            "filesDir/logs/lingxi.log",
                            fontSize = 12.sp
                        )
                    }
                )
            }
        )
    )
}

@Composable
private fun AboutCard() {
    SegmentedColumn(
        title = "关于",
        modifier = Modifier.fillMaxWidth(),
        content = listOf(
            {
                SegmentedListItem(
                    headlineContent = { Text("灵犀Hook", fontSize = 14.sp) },
                    supportingContent = { Text("基于 libxposed 的模块", fontSize = 12.sp) }
                )
            },
            {
                SegmentedListItem(
                    headlineContent = { Text("主题", fontSize = 14.sp) },
                    supportingContent = { Text("Material 3 Expressive", fontSize = 12.sp) }
                )
            }
        )
    )
}
