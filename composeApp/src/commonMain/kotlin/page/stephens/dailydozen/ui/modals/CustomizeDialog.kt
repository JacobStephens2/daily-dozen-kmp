package page.stephens.dailydozen.ui.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import page.stephens.dailydozen.domain.Categories
import page.stephens.dailydozen.ui.theme.DozenPalette

/** Customize Categories: pick a preset, or adjust per-category serving targets. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomizeDialog(
    initialServings: Map<String, Int>,
    onSave: (Map<String, Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    val working = remember {
        mutableStateMapOf<String, Int>().apply {
            Categories.all.forEach { put(it.id, initialServings[it.id] ?: 0) }
        }
    }
    val total by remember { derivedStateOf { working.values.sum() } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = DozenPalette.surface,
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Customize Categories", style = MaterialTheme.typography.headlineSmall, color = DozenPalette.primary)

            Text("Presets", style = MaterialTheme.typography.labelLarge, color = DozenPalette.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Categories.presets.values.forEach { preset ->
                    TextButton(
                        onClick = {
                            Categories.all.forEach { working[it.id] = preset.servings[it.id] ?: 0 }
                        },
                    ) { Text(preset.name) }
                }
            }

            Column(
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Categories.all.forEach { cat ->
                    val count = working[cat.id] ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth().alpha(if (count == 0) 0.5f else 1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${cat.icon}  ${cat.name}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        StepButton("−") { if (count > 0) working[cat.id] = count - 1 }
                        Text(
                            count.toString(),
                            modifier = Modifier.size(width = 28.dp, height = 24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = FontWeight.SemiBold,
                        )
                        StepButton("+") { if (count < 9) working[cat.id] = count + 1 }
                    }
                }
            }

            Text(
                "Total: $total servings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = { onSave(working.toMap()) }, modifier = Modifier.weight(1f)) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DozenPalette.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}
