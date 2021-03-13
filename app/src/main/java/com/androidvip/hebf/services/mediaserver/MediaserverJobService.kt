package com.androidvip.hebf.services.mediaserver

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import androidx.annotation.RequiresApi

import com.androidvip.hebf.utils.Logger
import com.androidvip.hebf.utils.RootUtils
import com.androidvip.hebf.utils.Utils

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class MediaserverJobService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        Utils.toggleMediaserverService(true, applicationContext)
        Logger.logInfo("Executing scheduled mediaserver kill…", applicationContext)
        RootUtils.executeAsync("killall -9 android.process.media", "killall -9 mediaserver")

        return false
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return false
    }

}