package com.wkusner.watermark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/** The Rising Tide screen: stays up until you log a drink or snooze. */
class NudgeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val s = Store(this)
        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Water break", style = MaterialTheme.typography.title2)
                    Text(
                        "${s.totalMl()} / ${s.goalMl} ml today",
                        style = MaterialTheme.typography.caption2,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Chip(
                        onClick = { Nudger.drink(this@NudgeActivity); finish() },
                        label = { Text("Drank a glass") },
                        colors = ChipDefaults.primaryChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Chip(
                        onClick = {
                            s.snoozeUntil = System.currentTimeMillis() + 30 * 60_000L
                            NotificationManagerCompat.from(this@NudgeActivity).cancel(Nudger.NOTIF_ID)
                            finish()
                        },
                        label = { Text("Snooze 30 min") },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
