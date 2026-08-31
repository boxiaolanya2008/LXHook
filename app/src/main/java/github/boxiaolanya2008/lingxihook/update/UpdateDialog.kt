package github.boxiaolanya2008.lingxihook.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 更新弹窗：forceUpdate=true 时无“下次再说”，false 时有。
 */
@Composable
fun UpdateDialog(
    info: UpdateInfo,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!info.forceUpdate) onDismiss() },
        title = {
            Column {
                Text(
                    "发现新版本 ${info.versionName}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (info.forceUpdate) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "此版本为强制更新",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("版本码：${info.versionCode}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(info.changelog.ifBlank { "暂无更新说明" }, fontSize = 13.sp, lineHeight = 18.sp)
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onUpdate) { Text("立即更新") }
                if (!info.forceUpdate) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) { Text("下次再说") }
                }
            }
        },
        dismissButton = null
    )
}
