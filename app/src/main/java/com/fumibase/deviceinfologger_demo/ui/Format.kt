package com.fumibase.deviceinfologger_demo.ui

import android.os.BatteryManager
import android.os.PowerManager
import com.fumibase.deviceinfologger_demo.data.db.DailySnapshot
import com.fumibase.deviceinfologger_demo.data.db.DeviceProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val UNSUPPORTED = "非対応"

inline fun <T> T?.orUnsupported(format: (T) -> String = { it.toString() }): String =
    if (this == null) UNSUPPORTED else format(this)

fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d時間%02d分%02d秒".format(h, m, s) else "%d分%02d秒".format(m, s)
}

fun formatDurationShort(ms: Long): String {
    val totalMin = ms / 60_000
    return "%d時間%02d分".format(totalMin / 60, totalMin % 60)
}

fun formatUahAsMah(uah: Int): String = "%,d mAh".format(uah / 1000)

fun formatBytes(bytes: Long): String {
    val gb = bytes / 1_000_000_000.0
    return "%.1f GB".format(gb)
}

fun formatDateTime(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN).format(Date(millis))

fun batteryHealthName(health: Int): String = when (health) {
    BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "過熱"
    BatteryManager.BATTERY_HEALTH_DEAD -> "劣化(寿命)"
    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "過電圧"
    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "不明な異常"
    BatteryManager.BATTERY_HEALTH_COLD -> "低温"
    BatteryManager.BATTERY_HEALTH_UNKNOWN -> "不明"
    else -> "不明($health)"
}

fun batteryStatusName(status: Int): String = when (status) {
    BatteryManager.BATTERY_STATUS_CHARGING -> "充電中"
    BatteryManager.BATTERY_STATUS_DISCHARGING -> "放電中"
    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "充電していない"
    BatteryManager.BATTERY_STATUS_FULL -> "満充電"
    BatteryManager.BATTERY_STATUS_UNKNOWN -> "不明"
    else -> "不明($status)"
}

fun pluggedName(plugged: Int): String = when (plugged) {
    0 -> "なし(バッテリー駆動)"
    BatteryManager.BATTERY_PLUGGED_AC -> "AC アダプター"
    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "ワイヤレス"
    BatteryManager.BATTERY_PLUGGED_DOCK -> "ドック"
    else -> "その他($plugged)"
}

fun thermalStatusName(status: Int): String = when (status) {
    PowerManager.THERMAL_STATUS_NONE -> "正常"
    PowerManager.THERMAL_STATUS_LIGHT -> "軽度"
    PowerManager.THERMAL_STATUS_MODERATE -> "中程度"
    PowerManager.THERMAL_STATUS_SEVERE -> "重度"
    PowerManager.THERMAL_STATUS_CRITICAL -> "危険"
    PowerManager.THERMAL_STATUS_EMERGENCY -> "緊急"
    PowerManager.THERMAL_STATUS_SHUTDOWN -> "シャットダウン寸前"
    else -> "不明($status)"
}

fun DeviceProfile.toRows(): List<Pair<String, String>> = listOf(
    "ID" to id.toString(),
    "作成日時" to formatDateTime(createdAt),
    "端末 ID (ANDROID_ID)" to deviceId.orUnsupported(),
    "インストール ID" to installId,
    "メーカー" to manufacturer,
    "ブランド" to brand,
    "モデル" to model,
    "デバイス" to device,
    "プロダクト" to product,
    "端末名" to deviceName.orUnsupported(),
    "SoC メーカー" to socManufacturer.orUnsupported(),
    "SoC モデル" to socModel.orUnsupported(),
    "ハードウェア" to hardware,
    "ボード" to board,
    "ABI" to abis,
    "CPU コア数" to cpuCores.toString(),
    "RAM" to totalRamBytes.orUnsupported { formatBytes(it) },
    "ストレージ" to totalStorageBytes.orUnsupported { formatBytes(it) },
    "画面サイズ" to if (screenWidthPx != null && screenHeightPx != null) "$screenWidthPx × $screenHeightPx px" else UNSUPPORTED,
    "画面密度" to densityDpi.orUnsupported { "$it dpi" },
    "リフレッシュレート" to refreshRates.orUnsupported { "$it Hz" },
    "カメラ数" to cameraCount.orUnsupported(),
    "センサー数" to sensorCount.orUnsupported(),
    "電池の種類" to batteryTechnology.orUnsupported(),
    "Android バージョン" to androidVersion,
    "SDK" to sdkInt.toString(),
    "セキュリティパッチ" to securityPatch.orUnsupported(),
    "Build fingerprint" to buildFingerprint,
)

fun DailySnapshot.toRows(): List<Pair<String, String>> = listOf(
    "ID" to id.toString(),
    "プロファイル ID" to profileId.toString(),
    "記録日時" to formatDateTime(recordedAt),
    "対象日" to targetDate,
    "補完記録" to if (isBackfill) "はい" else "いいえ",
    "電池残量" to batteryLevel.orUnsupported { "$it %" },
    "電池温度" to batteryTempC.orUnsupported { "%.1f ℃".format(it) },
    "電圧" to batteryVoltageMv.orUnsupported { "$it mV" },
    "ヘルス" to batteryHealth.orUnsupported { batteryHealthName(it) },
    "サイクル数" to cycleCount.orUnsupported { "$it 回" },
    "残り電荷" to chargeCounterUah.orUnsupported { formatUahAsMah(it) },
    "推定満充電容量" to estimatedFullUah.orUnsupported { formatUahAsMah(it) },
    "充電中" to isCharging.orUnsupported { if (it) "はい" else "いいえ" },
    "起動回数" to bootCount.orUnsupported(),
    "前回からの再起動" to rebootsSincePrev.orUnsupported { "$it 回" },
    "起動からの経過" to elapsedRealtimeMs.orUnsupported { formatDuration(it) },
    "稼働時間" to uptimeMs.orUnsupported { formatDuration(it) },
    "画面 ON 時間" to screenOnMs.orUnsupported { formatDuration(it) },
    "画面 ON 回数" to screenOnCount.orUnsupported { "$it 回" },
    "ロック解除回数" to unlockCount.orUnsupported { "$it 回" },
    "熱状態" to thermalStatus.orUnsupported { thermalStatusName(it) },
)
