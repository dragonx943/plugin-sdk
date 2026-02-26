package app.kotatsu.parser.yurigarden

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import fi.iki.elonen.NanoHTTPD
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import org.json.JSONArray
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import java.io.ByteArrayInputStream

class ProxyServer(port: Int = 8081) : NanoHTTPD(port) {

    private val client = OkHttpClient()

    override fun serve(session: IHTTPSession): Response {
        val url = session.parameters["url"]?.firstOrNull()
        val key = session.parameters["key"]?.firstOrNull()

        if (url == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Missing url")
        }

        val req = Request.Builder()
            .url(url)
            .header("Referer", "https://yurigarden.com/")
            .header("Origin", "https://yurigarden.com")
            .build()

        val resp = client.newCall(req).execute()
        val bodyBytes = resp.body?.bytes()
            ?: return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, "text/plain", "No body"
            )

        var resultBytes = bodyBytes

        if (!key.isNullOrEmpty()) {
            val bmp = BitmapFactory.decodeByteArray(bodyBytes, 0, bodyBytes.size)
            if (bmp != null) {
                val unscrambled = unscrambleImage(bmp, key)
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

    private fun unscrambleImage(bitmap: Bitmap, key: String): Bitmap {
        val js = """
			(function(Q0,Q1,Q2){"use strict";const A=(()=>{const L=[49,50,51,52,53,54,55,56,57,65,66,67,68,69,70,71,72,74,75,76,77,78,80,81,82,83,84,85,86,87,88,89,90,97,98,99,100,101,102,103,104,105,106,107,109,110,111,112,113,114,115,116,117,118,119,120,121,122];return L.map(c=>String.fromCharCode(c)).join("")})();const F=(()=>{let f=[1];for(let i=1;i<=10;i++)f[i]=f[i-1]*i;return f})();const _I=(E,P)=>{let n=[...Array(P).keys()],r=[];for(let a=P-1;a>=0;a--){let i=F[a],s=Math.floor(E/i);E%=i;r.push(n.splice(s,1)[0])}return r};const _S=str=>{let t=0;for(let ch of str){let r=A.indexOf(ch);if(r<0)throw Error("Invalid Base58 char");t=t*58+r}return t};const _U=(enc,p)=>{if(!/^H[1-9A-HJ-NP-Za-km-z]+$/.test(enc))throw Error("Bad Base58");let t=enc.slice(1,-1),n=enc.slice(-1),r=_S(t);if(A[r%58]!==n)throw Error("Checksum mismatch");return _I(r,p)};const _P=(h,p)=>{let n=Math.floor(h/p),r=h%p,a=[];for(let i=0;i<p;i++)a.push(n+(i<r?1:0));return a};const _D=e=>{let t=Array(e.length);e.forEach((v,i)=>t[v]=i);return t};const _X=(K,H,P)=>{let e=_U(K.slice(4),P),s=_D(e),u=_P(H-4*(P-1),P),m=e.map(i=>u[i]),pts=[0];for(let i=0;i<m.length;i++)pts[i+1]=pts[i]+m[i];let f=[];for(let i=0;i<m.length;i++)f.push({y:i?pts[i]+4*i:0,h:m[i]});return s.map(i=>f[i])};return _X(Q0,Q1,Q2)})("$key",${bitmap.height},10);
		""".trimIndent()

        val result = evaluateJs(js)
            ?: return bitmap

        val arr = JSONArray(result)
        val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        var dy = 0

        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val sy = o.getInt("y")
            val h = o.getInt("h")
            val src = Rect(0, sy, bitmap.width, sy + h)
            val dst = Rect(0, dy, bitmap.width, dy + h)
            canvas.drawBitmap(bitmap, src, dst, null)
            dy += h
        }

        return out
    }

    /**
     * Chạy đoạn JS bằng Rhino engine (không cần WebView).
     * Trả về kết quả dạng String, hoặc null nếu lỗi.
     */
    private fun evaluateJs(script: String): String? {
        return try {
            val ctx: Context = Context.enter()
            ctx.optimizationLevel = -1 // interpreter mode, hoạt động trên Android
            val scope: Scriptable = ctx.initStandardObjects()
            val result = ctx.evaluateString(scope, script, "unscramble", 1, null)
            Context.toString(result)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            Context.exit()
        }
    }
}