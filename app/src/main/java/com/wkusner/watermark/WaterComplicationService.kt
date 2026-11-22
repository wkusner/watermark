package com.wkusner.watermark

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

/** Water ring on the watch face. Tap it to log a glass. */
class WaterComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        build(type, total = 1250, goal = 2000, withTap = false)

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val s = Store(this)
        return build(request.complicationType, s.totalMl(), s.goalMl, withTap = true)
    }

    private fun build(type: ComplicationType, total: Int, goal: Int, withTap: Boolean): ComplicationData? {
        val label = if (total >= 1000) String.format("%.1fL", total / 1000f) else "$total"
        val text = PlainComplicationText.Builder(label).build()
        val desc = PlainComplicationText.Builder("$total of $goal ml of water today").build()
        val tap = if (withTap) PendingIntent.getBroadcast(
            this, 10,
            Intent(this, ActionReceiver::class.java).setAction(ActionReceiver.DRINK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        ) else null

        return when (type) {
            ComplicationType.RANGED_VALUE ->
                RangedValueComplicationData.Builder(
                    value = total.toFloat().coerceAtMost(goal.toFloat()),
                    min = 0f,
                    max = goal.toFloat(),
                    contentDescription = desc,
                ).setText(text).setTapAction(tap).build()

            ComplicationType.SHORT_TEXT ->
                ShortTextComplicationData.Builder(text, desc).setTapAction(tap).build()

            else -> null
        }
    }
}
