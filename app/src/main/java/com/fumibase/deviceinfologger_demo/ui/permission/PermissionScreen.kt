package com.fumibase.deviceinfologger_demo.ui.permission

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen(
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("使用状況へのアクセス", style = MaterialTheme.typography.headlineSmall)
        Text(
            "このアプリは、1日の画面 ON 時間・画面を点けた回数・ロック解除回数を記録するために、" +
                "Android の「使用状況へのアクセス」を利用します。\n\n" +
                "取得するのは画面の ON / OFF とロック解除のタイミングだけで、" +
                "どのアプリを使ったかは記録しません。データは端末内にのみ保存されます。",
        )
        Text(
            "次の画面の一覧からこのアプリを選び、「使用状況へのアクセスを許可」を ON にしてください。",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("設定画面を開く") }
        OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text("許可せずに使う")
        }
        Text(
            "許可しなくてもバッテリー・熱・起動の記録は行われます(画面 ON 時間などは空欄になります)。",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
