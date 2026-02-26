package app.kotatsu.parser.yurigarden

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import okio.Buffer

object ImageUnscrambler {

    private const val SLICE_COUNT = 10
    private const val GAP_PX = 4

    fun unscrambleToBytes(bytes: ByteArray, key: String): ByteArray? {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val result = runCatching { unscramble(bitmap, key) }.getOrNull() ?: return null
        val buffer = Buffer()
        result.compress(Bitmap.CompressFormat.PNG, 100, buffer.outputStream())
        return buffer.readByteArray()
    }

    private fun unscramble(bitmap: Bitmap, key: String): Bitmap {
        val slices = computeSlices(key, bitmap.height)
        val outH = slices.sumOf { it.h }
        val out = Bitmap.createBitmap(bitmap.width, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        var dy = 0
        for (s in slices) {
            canvas.drawBitmap(
                bitmap,
                Rect(0, s.y, bitmap.width, s.y + s.h),
                Rect(0, dy, bitmap.width, dy + s.h),
                null,
            )
            dy += s.h
        }
        return out
    }

    private data class Slice(val y: Int, val h: Int)

    private fun computeSlices(key: String, height: Int): List<Slice> {
        val p = SLICE_COUNT
        val alpha = buildString {
            intArrayOf(
                49,50,51,52,53,54,55,56,57,
                65,66,67,68,69,70,71,72,74,75,76,77,78,
                80,81,82,83,84,85,86,87,88,89,90,
                97,98,99,100,101,102,103,104,105,106,107,
                109,110,111,112,113,114,115,116,117,118,119,120,121,122,
            ).forEach { append(it.toChar()) }
        }

        val fact = LongArray(p + 1).also { f ->
            f[0] = 1L
            for (i in 1..p) f[i] = f[i - 1] * i
        }

        fun b58ToLong(s: String): Long {
            var t = 0L
            for (ch in s) t = t * 58 + alpha.indexOf(ch).also { require(it >= 0) { "Bad char: $ch" } }
            return t
        }

        fun factoradicDecode(n: Long, size: Int): IntArray {
            var rem = n
            val pool = (0 until size).toMutableList()
            val out = IntArray(size)
            for (a in size - 1 downTo 0) {
                val idx = (rem / fact[a]).toInt(); rem %= fact[a]
                out[size - 1 - a] = pool.removeAt(idx)
            }
            return out
        }

        fun decodeKey(enc: String): IntArray {
            require(enc.startsWith("H")) { "Key must start with H" }
            val r = b58ToLong(enc.substring(1, enc.length - 1))
            require(alpha[(r % 58).toInt()] == enc.last()) { "Checksum mismatch" }
            return factoradicDecode(r, p)
        }

        fun distribute(h: Int): IntArray {
            val base = h / p; val rem = h % p
            return IntArray(p) { i -> base + if (i < rem) 1 else 0 }
        }

        fun inverse(perm: IntArray): IntArray {
            val inv = IntArray(perm.size)
            perm.forEachIndexed { i, v -> inv[v] = i }
            return inv
        }

        val e = decodeKey(key.substring(4))
        val s = inverse(e)
        val u = distribute(height - GAP_PX * (p - 1))
        val m = IntArray(p) { i -> u[e[i]] }

        val f = Array(p) { Slice(0, 0) }
        var pts = 0
        for (i in 0 until p) {
            f[i] = Slice(y = pts, h = m[i])
            pts += m[i] + GAP_PX
        }

        return List(p) { i -> f[s[i]] }
    }
}