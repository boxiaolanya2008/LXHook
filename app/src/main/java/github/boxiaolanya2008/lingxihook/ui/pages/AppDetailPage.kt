package github.boxiaolanya2008.lingxihook.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.boxiaolanya2008.lingxihook.data.AppPrefs
import github.boxiaolanya2008.lingxihook.hook.HookFeature
import github.boxiaolanya2008.lingxihook.hook.HookRegistry
import github.boxiaolanya2008.lingxihook.ui.component.ExpressiveSwitch
import github.boxiaolanya2008.lingxihook.ui.component.SegmentedColumn
import github.boxiaolanya2008.lingxihook.ui.component.SegmentedListItem

@Composable
fun AppDetailPage(
    packageName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hooker = remember(packageName) { HookRegistry.find(packageName) } ?: return
    val context = LocalContext.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text("‹ 返回", fontSize = 14.sp)
            }
            Column {
                Text(hooker.label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CardDarkText)
                Text(hooker.packageName, fontSize = 12.sp, color = CardGrayText)
            }
        }

        SegmentedColumn(
            title = "功能开关",
            content = hooker.features.map { feature ->
                {
                    var enabled by remember(feature.key) {
                        mutableStateOf(AppPrefs.isFeatureEnabled(context, feature.key, feature.defaultEnabled))
                    }
                    FeatureRow(
                        feature = feature,
                        enabled = enabled,
                        onToggle = { value ->
                            enabled = value
                            AppPrefs.setFeatureEnabled(context, feature.key, value)
                        }
                    )
                }
            }
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FeatureRow(feature: HookFeature, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    SegmentedListItem(
        headlineContent = { Text(feature.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = CardDarkText) },
        supportingContent = { Text(feature.description, fontSize = 12.sp, lineHeight = 17.sp, color = CardGrayText) },
        trailingContent = {
            ExpressiveSwitch(checked = enabled, onCheckedChange = onToggle)
        }
    )
}
