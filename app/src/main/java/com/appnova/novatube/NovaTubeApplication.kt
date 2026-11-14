package com.appnova.novatube

import android.app.Application
import com.appnova.novatube.youtube.NewPipeDownloader
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.util.concurrent.Executors

class NovaTubeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NewPipe.init(NewPipeDownloader())
        // Pre-warm NewPipeExtractor's YouTube player JS cache in background
        // so the first video tap does not wait 4-6s for Rhino JS deciphering.
        Executors.newSingleThreadExecutor().execute {
            runCatching {
                StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
            }
        }
    }
}
