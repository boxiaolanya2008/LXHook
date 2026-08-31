package github.boxiaolanya2008.lingxihook.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val LocalSegmentedShapes = compositionLocalOf<ListItemShapes?> { null }

@Composable
fun SegmentedColumn(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: List<@Composable () -> Unit>
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
        content.forEachIndexed { index, item ->
            val shapes = ListItemDefaults.segmentedShapes(index = index, count = content.size)
            CompositionLocalProvider(LocalSegmentedShapes provides shapes) {
                AnimatedVisibility(
                    visible = true,
                    enter = expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(spring(stiffness = Spring.StiffnessMediumLow)),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
                ) {
                    item()
                }
            }
        }
    }
}

@Composable
fun SegmentedListItem(
    onClick: (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    headlineContent: @Composable () -> Unit,
    supportingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    shapes: ListItemShapes? = LocalSegmentedShapes.current
) {
    val resolvedShapes = shapes ?: ListItemDefaults.segmentedShapes(index = 0, count = 1)
    val colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    if (onClick != null) {
        androidx.compose.material3.SegmentedListItem(
            onClick = onClick,
            shapes = resolvedShapes,
            colors = colors,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            overlineContent = overlineContent,
            supportingContent = supportingContent,
            content = headlineContent
        )
    } else {
        androidx.compose.material3.SegmentedListItem(
            shapes = resolvedShapes,
            colors = colors,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            overlineContent = overlineContent,
            supportingContent = supportingContent,
            content = headlineContent
        )
    }
}
