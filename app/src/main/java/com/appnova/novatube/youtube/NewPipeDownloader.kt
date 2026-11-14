package com.appnova.novatube.youtube

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class NewPipeDownloader : Downloader() {

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val url = URL(request.url())
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = request.httpMethod()
            connectTimeout = 10_000
            readTimeout = 15_000
            useCaches = false
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            )
            request.headers().forEach { (key, values) ->
                values.forEach { value -> addRequestProperty(key, value) }
            }
        }

        val dataToSend = request.dataToSend()
        if (dataToSend != null) {
            conn.doOutput = true
            conn.outputStream.use { it.write(dataToSend) }
        }

        val responseCode = conn.responseCode
        if (responseCode == 429) {
            throw ReCaptchaException("reCaptcha Challenge requested", request.url())
        }

        val stream = if (responseCode >= 400) conn.errorStream else conn.inputStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        val latestUrl = conn.url?.toString() ?: request.url()
        return Response(responseCode, conn.responseMessage.orEmpty(), conn.headerFields, body, latestUrl)
    }
}
