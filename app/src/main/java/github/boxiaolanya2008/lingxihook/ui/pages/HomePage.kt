package github.boxiaolanya2008.lingxihook.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.boxiaolanya2008.lingxihook.hook.AppHooker
import github.boxiaolanya2008.lingxihook.hook.HookRegistry
import github.boxiaolanya2008.lingxihook.ui.component.SegmentedColumn
import github.boxiaolanya2008.lingxihook.ui.component.SegmentedListItem
import github.boxiaolanya2008.lingxihook.ui.component.TonalCard

internal val CardDarkText: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

internal val CardGrayText: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

@Deprecated("改用 SegmentedList 体系")
@Composable
internal fun WhiteCard(content: @Composable ColumnScope.() -> Unit) {
    TonalCard {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
internal fun CardTitle(text: String) {
    Text(text, fontSize = 12.sp, color = CardGrayText)
}

@Composable
fun HomePage(onOpenApp: (String) -> Unit, modifier: Modifier = Modifier) {
    val hookers = HookRegistry.all

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        if (hookers.isEmpty()) {
            SegmentedColumn(
                title = "已适配应用",
                content = listOf(
                    {
                        SegmentedListItem(
                            headlineContent = { Text("暂无适配", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = CardDarkText) },
                            supportingContent = { Text("当前版本未包含任何 Hook 适配，请等待更新", fontSize = 12.sp, color = CardGrayText) }
                        )
                    }
                )
            )
        } else {
            SegmentedColumn(
                title = "已适配应用",
                content = hookers.map { hooker ->
                    {
                        AppEntryItem(hooker = hooker, onClick = { onOpenApp(hooker.packageName) })
                    }
                }
            )
        }

        SegmentedColumn(
            title = "日志标记说明",
            content = listOf(
                {
                    SegmentedListItem(
                        headlineContent = { Text("日志分段", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = CardDarkText) },
                        supportingContent = {
                            Text(
                                "[powersaving] 省电管理注入\n" +
                                    "[wireless] 无线充电放行\n" +
                                    "[deepopt] 深度优化拦截\n" +
                                    "[camera] 相机总线注入\n" +
                                    "[zeiss] ZEISS 水印与机型伪装\n" +
                                    "[icons] 水印图标全显\n" +
                                    "[campus] 校园水印\n" +
                                    "[highpixel] 高像素解锁\n" +
                                    "[model] 机型伪装 PD2520→PD2502\n" +
                                    "[device] 系统/设置机型伪装\n" +
                                    "[update] 远程更新检查\n" +
                                    "[激活检测] 模块注入本应用",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = CardDarkText
                            )
                        }
                    )
                },
                {
                    SegmentedListItem(
                        headlineContent = { Text("提示", fontSize = 13.sp, color = CardDarkText) },
                        supportingContent = {
                            Text(
                                "以上日志都会自动落盘到「日志」页；logcat 仅用于实时调试。Root 相关操作失败会额外记录 [device] 警告。",
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = CardGrayText
                            )
                        }
                    )
                }
            )
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AppEntryItem(hooker: AppHooker, onClick: () -> Unit) {
    SegmentedListItem(
        onClick = onClick,
        headlineContent = {
            Text(hooker.label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = CardDarkText)
        },
        supportingContent = {
            Column {
                Text(hooker.packageName, fontSize = 12.sp, color = CardGrayText)
                Spacer(Modifier.height(2.dp))
                Text("${hooker.features.size} 个可开关功能 · 点击进入适配页", fontSize = 12.sp, color = CardGrayText)
            }
        },
        trailingContent = {
            Text("›", fontSize = 18.sp, color = CardGrayText)
        }
    )
}
