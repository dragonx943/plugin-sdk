package app.kotatsu.parser.mimihentai

import fi.iki.elonen.NanoHTTPD
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import java.io.ByteArrayInputStream

class ProxyServer(port: Int = 8080) : NanoHTTPD(port) {

    private val client = OkHttpClient()

    override fun serve(session: IHTTPSession): Response {
        val url = session.parameters["url"]?.firstOrNull()
        val drm = session.parameters["drm"]?.firstOrNull()

        if (url == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Missing url")
        }

        val req = Request.Builder().url(url).build()
        val resp = client.newCall(req).execute()
        val bodyBytes = resp.body?.bytes() ?: return newFixedLengthResponse(
            Response.Status.INTERNAL_ERROR, "text/plain", "No body"
        )

        var resultBytes = bodyBytes

        if (!drm.isNullOrEmpty()) {
            val bmp = BitmapFactory.decodeByteArray(bodyBytes, 0, bodyBytes.size)
            if (bmp != null) {
                val unscrambled = MimiHentaiParser.unscrambleImage(bmp, drm)
                val buffer = Buffer()
                unscrambled.compress(Bitmap.CompressFormat.PNG, 100, buffer.outputStream())
                resultBytes = buffer.readByteArray()
            }
        }

        return newFixedLengthResponse(
            Response.Status.OK,
            "image/png",
            ByteArrayInputStream(resultBytes),
            resultBytes.size.toLong()
        )
    }
}