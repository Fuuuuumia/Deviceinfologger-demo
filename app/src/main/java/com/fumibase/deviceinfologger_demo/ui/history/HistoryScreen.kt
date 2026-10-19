package com.fumibase.deviceinfologger_demo.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fumibase.deviceinfologger_demo.data.db.DailySnapshot
import com.fumibase.deviceinfologger_demo.ui.DetailDialog
import com.fumibase.deviceinfologger_demo.ui.formatDateTime
import com.fumibase.deviceinfologger_demo.ui.formatDurationShort
import com.fumibase.deviceinfologger_demo.ui.formatUahAsMah
import com.fumibase.deviceinfologger_demo.ui.orUnsupported
import com.fumibase.deviceinfologger_demo.ui.toRows

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onRecordRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snapshots by viewModel.snapshots.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<DailySnapshot?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${snapshots.size} 件", style = MaterialTheme.typography.titleMedium)
            Button(onClick = {
                viewModel.recordNow()
                onRecordRequested()
            }) { Text("今すぐ記録") }
        }

        if (snapshots.isEmpty()) {
            Text(
                "まだ記録がありません。「今すぐ記録」で1件追加できます。",
                modifier = Modifier.padding(16.dp),
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(snapshots, key = { it.id }) { snapshot ->
                SnapshotRow(snapshot, onClick = { selected = snapshot })
            }
        }
    }

    selected?.let {
        DetailDialog(title = "記録 ${it.targetDate}", rows = it.toRows(), onDismiss = { selected = null })
    }
}

@Composable
private fun SnapshotRow(snapshot: DailySnapshot, onClick: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(snapshot.targetDate, style = MaterialTheme.typography.titleMedium)
                if (snapshot.isBackfill) {
                    Text(
                        "  補完",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            Text(
                "記録: ${formatDateTime(snapshot.recordedAt)}",
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                listOf(
                    "残量 " + snapshot.batteryLevel.orUnsupported { "$it%" },
                    "サイクル " + snapshot.cycleCount.orUnsupported { "$it" },
                    "推定容量 " + snapshot.estimatedFullUah.orUnsupported { formatUahAsMah(it) },
                ).joinToString(" / "),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                listOf(
                    "画面ON " + snapshot.screenOnMs.orUnsupported { formatDurationShort(it) },
                    "再起動 " + snapshot.rebootsSincePrev.orUnsupported { "$it 回" },
                ).joinToString(" / "),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
