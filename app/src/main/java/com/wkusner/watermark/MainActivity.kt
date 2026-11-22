package com.wkusner.watermark

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        val store = Store(this)
        setContent {
            MaterialTheme {
                WatermarkScreen(
                    store = store,
                    onDrink = { Nudger.drink(this) },
                    onChange = { Nudger.refresh(this) },
                )
            }
        }
    }
}

private fun hhmm(min: Int) = "%d:%02d".format(min / 60, min % 60)

@Composable
fun WatermarkScreen(store: Store, onDrink: () -> Unit, onChange: () -> Unit) {
    var tick by remember { mutableIntStateOf(0) }
    fun changed() { tick++; onChange() }
    // Read everything once per recomposition (tick forces a fresh read after any change).
    val total = remember(tick) { store.totalMl() }
    val goal = remember(tick) { store.goalMl }
    val glass = remember(tick) { store.glassMl }
    val wake = remember(tick) { store.wakeMin }
    val cutoff = remember(tick) { store.cutoffMin }
    val recent = remember(tick) { store.recentWindowMin }
    val rising = remember(tick) { store.risingTide }

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                "$total / $goal ml",
                style = MaterialTheme.typography.title2,
                textAlign = TextAlign.Center,
            )
        }
        item {
            Chip(
                onClick = { onDrink(); tick++ },
                label = { Text("+1 glass ($glass ml)") },
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            CompactChip(onClick = { store.undo(); changed() }, label = { Text("Undo") })
        }
        item { Stepper("Daily goal", "$goal ml", { store.goalMl -= 250; changed() }, { store.goalMl += 250; changed() }) }
        item { Stepper("Glass size", "$glass ml", { store.glassMl -= 50; changed() }, { store.glassMl += 50; changed() }) }
        item { Stepper("Wake", hhmm(wake), { store.wakeMin -= 30; changed() }, { store.wakeMin += 30; changed() }) }
        item { Stepper("Quiet after", hhmm(cutoff), { store.cutoffMin -= 30; changed() }, { store.cutoffMin += 30; changed() }) }
        item { Stepper("Wait after a drink", "$recent min", { store.recentWindowMin -= 15; changed() }, { store.recentWindowMin += 15; changed() }) }
        item {
            ToggleChip(
                checked = rising,
                onCheckedChange = { store.risingTide = it; changed() },
                label = { Text("Rising Tide") },
                secondaryLabel = { Text("Nudges get firmer") },
                toggleControl = {
                    Icon(
                        imageVector = ToggleChipDefaults.switchIcon(checked = rising),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactChip(onClick = onMinus, label = { Text("−") })
        Text("$label\n$value", textAlign = TextAlign.Center, style = MaterialTheme.typography.caption2)
        CompactChip(onClick = onPlus, label = { Text("+") })
    }
}
