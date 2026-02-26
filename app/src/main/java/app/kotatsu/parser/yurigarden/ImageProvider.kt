package app.kotatsu.parser.yurigarden

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import app.kotatsu.plugin.sdk.network.CommonHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.concurrent.thread

class ImageProvider : ContentProvider() {

    private val domain = "yurigarden.com"

    private val client by lazy {
        OkHttpClient()
    }

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val url = uri.getQueryParameter("url")
            ?: throw IllegalArgumentException("Missing url param")
        val key = uri.getQueryParameter("key")
        val (readFd, writeFd) = ParcelFileDescriptor.createPipe()

        thread(isDaemon = true) {
            try {
                ParcelFileDescriptor.AutoCloseOutputStream(writeFd).use { out ->
                    val req = Request.Builder()
                        .url(url)
                        .header(CommonHeaders.REFERER, "https://$domain/")
                        .header("Origin", "https://$domain")
                        .header(CommonHeaders.USER_AGENT, "Kotatsu/9.0 (Android 16;;; en)")
                        .build()

                    val bytes = client.newCall(req).execute().use { resp ->
                        resp.body?.bytes() ?: return@thread
                    }

                    val result = if (!key.isNullOrEmpty()) {
                        ImageUnscrambler.unscrambleToBytes(bytes, key) ?: bytes
                    } else {
                        bytes
                    }

                    out.write(result)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runCatching { writeFd.close() }
            }
        }

        return readFd
    }

    override fun query(uri: Uri, p: Array<String>?, s: String?, a: Array<String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String = "image/png"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<String>?): Int = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<String>?): Int = 0
}