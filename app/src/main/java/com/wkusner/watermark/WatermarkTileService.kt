package com.wkusner.watermark

import androidx.core.app.NotificationManagerCompat
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.CircularProgressIndicator
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.ProgressIndicatorColors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.EdgeContentLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/** Swipe-to tile: progress ring, today's total, +1 glass, undo. */
class WatermarkTileService : TileService() {

    private val water = argb(0xFF4FC3F7.toInt())
    private val track = argb(0xFF1C2B33.toInt())
    private val white = argb(0xFFFFFFFF.toInt())
    private val grey = argb(0xFFAAAAAA.toInt())

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        val s = Store(this)
        when (requestParams.currentState.lastClickableId) {
            "add" -> {
                s.addDrink()
                NotificationManagerCompat.from(this).cancel(Nudger.NOTIF_ID)
                Nudger.refresh(this, includeTile = false)
            }
            "undo" -> {
                s.undo()
                Nudger.refresh(this, includeTile = false)
            }
        }

        val total = s.totalMl()
        val goal = s.goalMl
        val dp = requestParams.deviceConfiguration

        val addClick = ModifiersBuilders.Clickable.Builder()
            .setId("add")
            .setOnClick(ActionBuilders.LoadAction.Builder().build())
            .build()
        val undoClick = ModifiersBuilders.Clickable.Builder()
            .setId("undo")
            .setOnClick(ActionBuilders.LoadAction.Builder().build())
            .build()

        val center = LayoutElementBuilders.Column.Builder()
            .addContent(
                Text.Builder(this, "$total")
                    .setTypography(Typography.TYPOGRAPHY_DISPLAY3)
                    .setColor(white)
                    .build()
            )
            .addContent(
                Text.Builder(this, "of $goal ml")
                    .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                    .setColor(grey)
                    .build()
            )
            .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(6f)).build())
            .addContent(CompactChip.Builder(this, "+1 glass", addClick, dp).build())
            .build()

        val layout = EdgeContentLayout.Builder(dp)
            .setEdgeContent(
                CircularProgressIndicator.Builder()
                    .setProgress((total.toFloat() / goal).coerceIn(0f, 1f))
                    .setCircularProgressIndicatorColors(ProgressIndicatorColors(water, track))
                    .build()
            )
            .setPrimaryLabelTextContent(
                Text.Builder(this, "Watermark")
                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                    .setColor(water)
                    .build()
            )
            .setContent(center)
            .setSecondaryLabelTextContent(
                Text.Builder(this, "undo")
                    .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                    .setColor(grey)
                    .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(undoClick).build())
                    .build()
            )
            .build()

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(15 * 60_000L)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
            .build()
        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(
            ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
        )

    companion object {
        private const val RESOURCES_VERSION = "1"
    }
}
