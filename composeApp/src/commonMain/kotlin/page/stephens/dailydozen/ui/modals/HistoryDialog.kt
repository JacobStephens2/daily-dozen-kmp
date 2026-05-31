package page.stephens.dailydozen.ui.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.datetime.LocalDate
import page.stephens.dailydozen.domain.DateKeys
import page.stephens.dailydozen.domain.HistoryCell
import page.stephens.dailydozen.ui.theme.DozenPalette

@Composable
fun HistoryDialog(
    initialMonth: Pair<Int, Int>,
    monthProvider: (year: Int, month: Int) -> page.stephens.dailydozen.domain.HistoryMonth,
    onPickDate: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var ym by remember { mutableStateOf(initialMonth) }
    val month = monthProvider(ym.first, ym.second)

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = DozenPalette.surface) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("History", style = MaterialTheme.typography.headlineSmall, color = DozenPalette.primary)
                    TextButton(onClick = onDismiss) { Text("Close") }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Stat(month.streak.toString(), "Day Streak")
                    Stat(month.perfectDays.toString(), "Perfect Days")
                    Stat(month.daysTracked.toString(), "Days Tracked")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = {
                        ym = if (ym.second == 1) (ym.first - 1) to 12 else ym.first to (ym.second - 1)
                    }) { Text("←") }
                    Text(month.label, fontWeight = FontWeight.SemiBold)
                    TextButton(
                        onClick = {
                            ym = if (ym.second == 12) (ym.first + 1) to 1 else ym.first to (ym.second + 1)
                        },
                        enabled = !month.isCurrentMonth,
                    ) { Text("→") }
                }

                // Weekday headers
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach {
                        Text(
                            it,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = DozenPalette.onSurfaceVariant,
                        )
                    }
                }

                // Calendar grid: pad leading blanks, then cells, in rows of 7.
                val slots: List<HistoryCell?> = buildList {
                    repeat(month.leadingBlanks) { add(null) }
                    addAll(month.cells)
                    while (size % 7 != 0) add(null)
                }
                slots.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { cell ->
                            Box(modifier = Modifier.weight(1f)) {
                                if (cell != null) DayCell(cell) { onPickDate(it); onDismiss() }
                            }
                        }
                    }
                }

                Text(
                    "Tap a day to edit its entries",
                    style = MaterialTheme.typography.bodySmall,
                    color = DozenPalette.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = DozenPalette.primary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = DozenPalette.onSurfaceVariant)
    }
}

@Composable
private fun DayCell(cell: HistoryCell, onClick: (LocalDate) -> Unit) {
    val bg = when {
        cell.isFuture || !cell.hasData -> DozenPalette.historyNone
        cell.percent >= 100 -> DozenPalette.historyFull
        cell.percent >= 75 -> DozenPalette.historyHigh
        cell.percent >= 50 -> DozenPalette.historyMid
        cell.percent > 0 -> DozenPalette.historyLow
        else -> DozenPalette.historyNone
    }
    val fg = if (cell.hasData && cell.percent >= 75) Color.White else DozenPalette.onSurface
    var mod = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .clip(RoundedCornerShape(8.dp))
        .background(bg)
    if (!cell.isFuture) mod = mod.clickable { onClick(cell.date) }
    Box(modifier = mod, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                cell.day.toString(),
                color = if (cell.isFuture) DozenPalette.outlineVariant else fg,
                fontSize = 13.sp,
                fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Normal,
            )
            if (cell.hasData) {
                Text("${cell.percent}%", color = fg, fontSize = 9.sp)
            }
        }
    }
}
