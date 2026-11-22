package com.wkusner.watermark

import android.app.Application
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class WatermarkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Nudger.createChannels(this)
        // Android's shortest background interval is 15 minutes.
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "watermark-check",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<NudgeWorker>(15, TimeUnit.MINUTES).build(),
        )
    }
}

class NudgeWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        Nudger.check(applicationContext)
        Nudger.refresh(applicationContext)
        return Result.success()
    }
}
