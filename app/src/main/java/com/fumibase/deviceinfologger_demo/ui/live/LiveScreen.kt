package com.fumibase.deviceinfologger_demo.ui.live

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fumibase.deviceinfologger_demo.data.db.DeviceProfile
import com.fumibase.deviceinfologger_demo.ui.DetailDialog
import com.fumibase.deviceinfologger_demo.ui.InfoCard
import com.fumibase.deviceinfologger_demo.ui.InfoRow
import com.fumibase.deviceinfologger_demo.ui.UNSUPPORTED
import com.fumibase.deviceinfologger_demo.ui.batteryHealthName
import com.fumibase.deviceinfologger_demo.ui.batteryStatusName
import com.fumibase.deviceinfologger_demo.ui.formatDuration
import com.fumibase.deviceinfologger_demo.ui.formatUahAsMah
import com.fumibase.deviceinfologger_demo.ui.orUnsupported
import com.fumibase.deviceinfologger_demo.ui.pluggedName
import com.fumibase.deviceinfologger_demo.ui.thermalStatusName
import com.fumibase.deviceinfologger_demo.ui.toRows

@Composable
fun LiveScreen(
    viewModel: LiveViewModel,
    onOpenPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.startMonitoring()
                Lifecycle.Event.ON_STOP -> viewModel.stopMonitoring()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopMonitoring()
        }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    var showProfile by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DeviceCard(profile = profile, onClick = { showProfile = true })

        if (!state.hasUsagePermission) {
            PermissionBanner(onOpenPermission)
        }

        BatteryCard(state)
        ThermalCard(state)
        BootCard(state)
        ScreenCard(state)
    }

    if (showProfile) {
        profile?.let {
            DetailDialog(title = "端末プロファイル", rows = it.toRows(), onDismiss = { showProfile = false })
        }
    }
}

@Composable
private fun DeviceCard(profile: DeviceProfile?, onClick: () -> Unit) {
    InfoCard(title = "端末", modifier = Modifier.clickable(onClick = onClick)) {
        if (profile == null) {
            Text("読み込み中…")
        } else {
            InfoRow("メーカー", profile.manufacturer)
            InfoRow("モデル", profile.model)
            InfoRow(
                "SoC",
                listOfNotNull(profile.socManufacturer, profile.socModel)
                    .joinToString(" ").ifEmpty { UNSUPPORTED },
            )
            InfoRow("Android", "${profile.androidVersion} (API ${profile.sdkInt})")
            Text(
                "タップで全項目を表示",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PermissionBanner(onOpenPermission: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("使用状況へのアクセスが未許可のため、画面 ON 時間・ロック解除回数は取得できません。")
            TextButton(onClick = onOpenPermission) { Text("設定方法を見る") }
        }
    }
}

@Composable
private fun BatteryCard(state: LiveUiState) {
    val b = state.battery
    InfoCard(title = "バッテリー") {
        InfoRow("残量", b?.level.orUnsupported { "$it %" })
        InfoRow("温度", b?.tempC.orUnsupported { "%.1f ℃".format(it) })
        InfoRow("電圧", b?.voltageMv.orUnsupported { "$it mV" })
        InfoRow("充電状態", b?.status.orUnsupported { batteryStatusName(it) })
        InfoRow("給電元", b?.plugged.orUnsupported { pluggedName(it) })
        InfoRow("ヘルス", b?.health.orUnsupported { batteryHealthName(it) })
        InfoRow(
            "瞬時電流",
            state.currentNowRaw.orUnsupported { "${it / 1000} mA(生値 $it)" },
        )
        InfoRow("サイクル数", b?.cycleCount.orUnsupported { "$it 回" })
        InfoRow("残り電荷", b?.chargeCounterUah.orUnsupported { formatUahAsMah(it) })
        InfoRow("推定満充電容量", b?.estimatedFullUah.orUnsupported { formatUahAsMah(it) })
        Text(
            "推定満充電容量は残量 50% 以上のときのみ表示",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun ThermalCard(state: LiveUiState) {
    InfoCard(title = "熱") {
        InfoRow("熱状態", state.thermalStatus.orUnsupported { thermalStatusName(it) })
        InfoRow("熱的余裕(10秒後予測)", state.thermalHeadroom.orUnsupported { "%.2f".format(it) })
        Text("熱的余裕は 1.0 に近づくほど性能制限が近い", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun BootCard(state: LiveUiState) {
    val t = state.bootTimes
    InfoCard(title = "起動") {
        InfoRow("起動からの経過時間", t?.elapsedRealtimeMs.orUnsupported { formatDuration(it) })
        InfoRow("稼働時間", t?.uptimeMs.orUnsupported { formatDuration(it) })
        InfoRow("スリープ時間", t?.sleepMs.orUnsupported { formatDuration(it) })
        InfoRow("累計起動回数", state.bootCount.orUnsupported { "$it 回" })
    }
}

@Composable
private fun ScreenCard(state: LiveUiState) {
    val s = state.todayScreen
    InfoCard(title = "画面(今日)") {
        InfoRow("画面 ON 時間", s?.screenOnMs.orUnsupported { formatDuration(it) })
        InfoRow("画面 ON 回数", s?.screenOnCount.orUnsupported { "$it 回" })
        InfoRow("ロック解除回数", s?.unlockCount.orUnsupported { "$it 回" })
    }
}
