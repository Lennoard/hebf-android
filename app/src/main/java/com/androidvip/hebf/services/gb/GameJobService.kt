package com.androidvip.hebf.services.gb

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import androidx.annotation.RequiresApi
import com.androidvip.hebf.utils.vip.VipBatterySaverNutellaImpl
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class GameJobService : JobService(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob()

    override fun onStartJob(params: JobParameters): Boolean {
        throw NotImplementedError()
    }

    override fun onStopJob(params: JobParameters): Boolean {
        coroutineContext[Job]?.cancelChildren()
        return false
    }

    override fun onDestroy() {
        coroutineContext[Job]?.cancelChildren()
        super.onDestroy()
    }

}