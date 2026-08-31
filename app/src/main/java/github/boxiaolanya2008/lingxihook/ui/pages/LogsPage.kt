package github.boxiaolanya2008.lingxihook.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.boxiaolanya2008.lingxihook.data.LogEntry
import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.data.LogRepo
import github.boxiaolanya2008.lingxihook.ui.component.SegmentedColumn
import github.boxiaolanya2008.lingxihook.ui.component.SegmentedListItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val levelColor: (LogLevel) -> Color = {
    when (it) {
        LogLevel.INFO -> Color(0xFF1E7B34)
        LogLevel.WARN -> Color(0xFFE65100)
        LogLevel.ERROR -> Color(0xFFC62828)
    }
}

@Composable
fun LogsPage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(LogRepo.readAll(context)) }
    var filter by remember { mutableStateOf<LogLevel?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh) { entries = LogRepo.readAll(context) }

    Column(modifier.padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "运行日志",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CardDarkText,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { refresh++ }) { Text("刷新") }
            TextButton(onClick = { LogRepo.clear(context); refresh++ }) { Text("清空") }
        }

        val filterOptions: List<Pair<LogLevel?, String>> = listOf(
            null to "全部",
            LogLevel.INFO to "INFO",
            LogLevel.WARN to "WARN",
            LogLevel.ERROR to "ERROR"
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            filterOptions.forEachIndexed { index, (level, label) ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = filterOptions.size),
                    selected = filter == level,
                    onClick = { filter = level }
                ) {
                    Text(label, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        val shown = if (filter == null) entries else entries.filter { it.level == filter }
        if (shown.isEmpty()) {
            SegmentedColumn(
                title = null,
                content = listOf(
                    {
                        SegmentedListItem(
                            headlineContent = { Text("暂无日志", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = CardDarkText) },
                            supportingContent = {
                                Text(
                                    "本页展示保存在应用内部存储的全部 Hook 日志：目标应用进程的日志通过内置广播通道自动回传落盘。\n" +
                                        "logcat（adb logcat -s LingXiHook）仅用于实时调试。",
                                    fontSize = 12.sp, lineHeight = 17.sp, color = CardGrayText
                                )
                            }
                        )
                    }
                )
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                itemsIndexed(shown) { idx, entry ->
                    LogRow(entry = entry, index = idx, count = shown.size)
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry, index: Int, count: Int) {
    val shapes = ListItemDefaults.segmentedShapes(index = index, count = count)
    SegmentedListItem(
        shapes = shapes,
        leadingContent = {
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(levelColor(entry.level))
            )
        },
        headlineContent = {
            Text(
                "${formatLogTime(entry.time)} · ${entry.tag} · ${entry.level.name}",
                fontSize = 11.sp,
                color = CardGrayText
            )
        },
        supportingContent = {
            Text(entry.message, fontSize = 13.sp, lineHeight = 18.sp, color = CardDarkText)
        }
    )
}

internal fun formatLogTime(millis: Long): String =
    SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))
