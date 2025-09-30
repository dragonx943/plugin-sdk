package app.kotatsu.parser.mimihentai

import app.kotatsu.plugin.sdk.core.Manga
import app.kotatsu.plugin.sdk.core.MangaChapter
import app.kotatsu.plugin.sdk.core.MangaListFilter
import app.kotatsu.plugin.sdk.core.MangaListFilterCapabilities
import app.kotatsu.plugin.sdk.core.MangaListFilterOptions
import app.kotatsu.plugin.sdk.core.MangaPage
import app.kotatsu.plugin.sdk.core.MangaParser
import app.kotatsu.plugin.sdk.core.MangaTag
import app.kotatsu.plugin.sdk.core.SortOrder
import app.kotatsu.plugin.sdk.network.Paginator
import app.kotatsu.plugin.sdk.util.generateUid
import app.kotatsu.plugin.sdk.util.json.getStringOrNull
import app.kotatsu.plugin.sdk.util.json.mapJSON
import app.kotatsu.plugin.sdk.util.json.mapJSONToSet
import app.kotatsu.plugin.sdk.util.parseJson
import app.kotatsu.plugin.sdk.util.parseJsonArray
import app.kotatsu.plugin.sdk.util.toAbsoluteUrl
import app.kotatsu.plugin.sdk.util.toTitleCase
import app.kotatsu.plugin.sdk.util.urlEncoded
import okhttp3.HttpUrl.Companion.toHttpUrl
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import fi.iki.elonen.NanoHTTPD.SOCKET_READ_TIMEOUT
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.EnumSet
import java.util.Locale
import kotlin.math.PI

class MimiHentaiParser(
    authority: String,
) : MangaParser(authority) {

    private val paginator = Paginator(initialPageSize = 18)
    private val sourceLocale = Locale.Builder().setLanguage("vi").setRegion("VN").build()

    override val filterCapabilities = MangaListFilterCapabilities(
        availableSortOrders = EnumSet.of(
            SortOrder.UPDATED,
            SortOrder.ALPHABETICAL,
            SortOrder.POPULARITY,
            SortOrder.POPULARITY_TODAY,
            SortOrder.POPULARITY_WEEK,
            SortOrder.POPULARITY_MONTH,
            SortOrder.RATING,
        ),
        isMultipleTagsSupported = true,
        isSearchSupported = true,
        isSearchWithFiltersSupported = true,
        isTagsExclusionSupported = true,
        isAuthorSearchSupported = true,
    )

    override val domain = "mimihentai.com"
    private val apiSuffix = "api/v1/manga"

    init {
        paginator.firstPage = 0
        // start local proxy server
        if (server == null) {
            server = ProxyServer()
            server!!.start(SOCKET_READ_TIMEOUT, false)
        }
    }

    override fun getFilterOptions() = MangaListFilterOptions(
        availableTags = fetchTags(),
    )

    override fun getList(offset: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val url = buildString {
            append("https://")
            append("$domain/$apiSuffix")

            if (!filter.query.isNullOrEmpty() || !filter.author.isNullOrEmpty() || filter.tags.isNotEmpty()) {
                append("/advance-search?page=")
                append(offset / 18)
                append("&max=18")

                if (!filter.query.isNullOrEmpty()) {
                    append("&name=")
                    append(checkNotNull(filter.query).urlEncoded())
                }

                if (!filter.author.isNullOrEmpty()) {
                    append("&author=")
                    append(filter.author?.urlEncoded())
                }

                if (filter.tags.isNotEmpty()) {
                    append("&genre=")
                    append(filter.tags.joinToString(",") { it.key })
                }

                if (filter.tagsExclude.isNotEmpty()) {
                    append("&ex=")
                    append(filter.tagsExclude.joinToString(",") { it.key })
                }

                append("&sort=")
                append(
                    when (order) {
                        SortOrder.UPDATED -> "updated_at"
                        SortOrder.ALPHABETICAL -> "title"
                        SortOrder.POPULARITY -> "follows"
                        SortOrder.POPULARITY_TODAY,
                        SortOrder.POPULARITY_WEEK,
                        SortOrder.POPULARITY_MONTH -> "views"
                        SortOrder.RATING -> "likes"
                        else -> ""
                    }
                )
            } else {
                append(
                    when (order) {
                        SortOrder.UPDATED -> "/tatcatruyen?page=${offset / 18}&sort=updated_at"
                        SortOrder.ALPHABETICAL -> "/tatcatruyen?page=${offset / 18}&sort=title"
                        SortOrder.POPULARITY -> "/tatcatruyen?page=${offset / 18}&sort=follows"
                        SortOrder.POPULARITY_TODAY -> "/tatcatruyen?page=${offset / 18}&sort=views"
                        SortOrder.POPULARITY_WEEK -> "/top-manga?page=${offset / 18}&timeType=1&limit=18"
                        SortOrder.POPULARITY_MONTH -> "/top-manga?page=${offset / 18}&timeType=2&limit=18"
                        SortOrder.RATING -> "/tatcatruyen?page=${offset / 18}&sort=likes"
                        else -> "/tatcatruyen?page=${offset / 18}&sort=updated_at" // default
                    }
                )

                if (filter.tagsExclude.isNotEmpty()) {
                    append("&ex=")
                    append(filter.tagsExclude.joinToString(",") { it.key })
                }
            }
        }

        val raw = webClient.httpGet(url.toHttpUrl())
        return if (url.contains("/top-manga")) {
            val data = raw.parseJsonArray()
            parseTopMangaList(data)
        } else {
            val data = raw.parseJson().getJSONArray("data")
            parseMangaList(data)
        }
    }

    private fun parseTopMangaList(data: JSONArray): List<Manga> {
        return data.mapJSON { jo ->
            val id = jo.getLong("id")
            val title = jo.getString("title").takeIf { it.isNotEmpty() } ?: "Web chưa đặt tên"
            val description = jo.getStringOrNull("description")

            val differentNames = mutableSetOf<String>().apply {
                jo.optJSONArray("differentNames")?.let { namesArray ->
                    for (i in 0 until namesArray.length()) {
                        namesArray.optString(i)?.takeIf { it.isNotEmpty() }?.let { name ->
                            add(name)
                        }
                    }
                }
            }

            val authors = jo.optJSONArray("authors")?.let { authorsArray ->
                val authorNames = mutableListOf<String>()
                for (i in 0 until authorsArray.length()) {
                    authorsArray.optJSONObject(i)?.getString("name")?.takeIf { it.isNotEmpty() }
                        ?.let { name ->
                            authorNames.add(name)
                        }
                }
                authorNames.joinToString(", ").takeIf { it.isNotEmpty() }
            }

            val tags = jo.getJSONArray("genres").mapJSON { genre ->
                MangaTag(
                    key = genre.getLong("id").toString(),
                    title = genre.getString("name"),
                )
            }.toSet()

            Manga(
                id = generateUid(id),
                title = title,
                altTitle = differentNames.joinToString(", ").takeIf { it.isNotEmpty() },
                url = "/$apiSuffix/info/$id",
                publicUrl = "https://$domain/g/$id",
                isNsfw = true,
                coverUrl = jo.getString("coverUrl"),
                state = null,
                description = description,
                tags = tags,
                author = authors,
                rating = 0f,
            )
        }
    }

    private fun parseMangaList(data: JSONArray): List<Manga> {
        return data.mapJSON { jo ->
            val id = jo.getLong("id")
            val title = jo.getString("title").takeIf { it.isNotEmpty() } ?: "Web chưa đặt tên"
            val description = jo.getStringOrNull("description")

            val differentNames = mutableSetOf<String>().apply {
                jo.optJSONArray("differentNames")?.let { namesArray ->
                    for (i in 0 until namesArray.length()) {
                        namesArray.optString(i)?.takeIf { it.isNotEmpty() }?.let { name ->
                            add(name)
                        }
                    }
                }
            }

            val authors = jo.optJSONArray("authors")?.let { authorsArray ->
                val authorNames = mutableListOf<String>()
                for (i in 0 until authorsArray.length()) {
                    authorsArray.optJSONObject(i)?.getString("name")?.takeIf { it.isNotEmpty() }
                        ?.let { name ->
                            authorNames.add(name)
                        }
                }
                authorNames.joinToString(", ").takeIf { it.isNotEmpty() }
            }

            val tags = jo.getJSONArray("genres").mapJSON { genre ->
                MangaTag(
                    key = genre.getLong("id").toString(),
                    title = genre.getString("name"),
                )
            }.toSet()

            Manga(
                id = generateUid(id),
                title = title,
                altTitle = differentNames.joinToString(", ").takeIf { it.isNotEmpty() },
                url = "/$apiSuffix/info/$id",
                publicUrl = "https://$domain/g/$id",
                rating = 0f,
                isNsfw = true,
                coverUrl = jo.getString("coverUrl"),
                state = null,
                tags = tags,
                description = description,
                author = authors,
            )
        }
    }

    override fun getDetails(url: String): Manga {
        val jo = webClient.httpGet(url.toAbsoluteUrl(domain).toHttpUrl()).parseJson()
        val id = jo.getLong("id")
        val title = jo.getString("title").takeIf { it.isNotEmpty() } ?: "Web chưa đặt tên"
        val description = jo.optString("description").takeIf { it.isNotEmpty() }

        val differentNames = mutableSetOf<String>().apply {
            jo.optJSONArray("differentNames")?.let { namesArray ->
                for (i in 0 until namesArray.length()) {
                    namesArray.optString(i)?.takeIf { it.isNotEmpty() }?.let { name ->
                        add(name)
                    }
                }
            }
        }

        val authors = jo.optJSONArray("authors")?.let { authorsArray ->
            val authorNames = mutableListOf<String>()
            for (i in 0 until authorsArray.length()) {
                authorsArray.optJSONObject(i)?.getString("name")?.takeIf { it.isNotEmpty() }
                    ?.let { name ->
                        authorNames.add(name)
                    }
            }
            authorNames.joinToString(", ").takeIf { it.isNotEmpty() }
        }

        val tags = jo.getJSONArray("genres").mapJSON { genre ->
            MangaTag(
                key = genre.getLong("id").toString(),
                title = genre.getString("name"),
            )
        }.toSet()

        return Manga(
            id = generateUid(id),
            title = title,
            altTitle = differentNames.joinToString(", ").takeIf { it.isNotEmpty() },
            url = "/$apiSuffix/info/$id",
            publicUrl = "https://$domain/g/$id",
            isNsfw = true,
            coverUrl = jo.getString("coverUrl"),
            state = null,
            description = description,
            tags = tags,
            author = authors,
            rating = 0f,
        )
    }

    override fun getChapters(url: String): List<MangaChapter> {
        val jo = webClient.httpGet(url.toAbsoluteUrl(domain).toHttpUrl()).parseJson()
        val id = jo.getLong("id")
        val uploaderName = jo.getJSONObject("uploader").getString("displayName")

        // for get chapters
        val urlChaps = "/$apiSuffix/gallery/$id".toAbsoluteUrl(domain).toHttpUrl()
        val parsedChapters = webClient.httpGet(urlChaps).parseJsonArray()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US)
        return parsedChapters.mapJSON { j ->
            MangaChapter(
                id = generateUid(j.getLong("id")),
                name = j.getString("title"),
                number = j.getString("order").toFloat(),
                url = "$apiSuffix/chapter?id=${j.getLong("id")}",
                uploadDate = dateFormat.parse(j.getString("createdAt"))?.time ?: 0L,
                scanlator = uploaderName,
                branch = null,
                volume = 0,
            )
        }.reversed()
    }

    override fun getPages(url: String): List<MangaPage> {
        val json = webClient.httpGet("https://$domain/$url".toHttpUrl()).parseJson()
        return json.getJSONArray("pages").mapJSON { jo ->
            val imageUrl = jo.getString("imageUrl")
            val drm = jo.getStringOrNull("drm")

            val proxyUrl = if (drm != null) {
                "$LOCAL_PROXY?url=${imageUrl.urlEncoded()}&drm=${drm.urlEncoded()}"
            } else {
                "$LOCAL_PROXY?url=${imageUrl.urlEncoded()}"
            }

            MangaPage(
                id = generateUid(imageUrl),
                url = proxyUrl,
                preview = null,
            )
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val newRequest = chain.request().newBuilder()
            .header("User-Agent", "Kotatsu/6.8 (Android 13;;; en)")
            .build()

        return super.intercept(object : Interceptor.Chain by chain {
            override fun request() = newRequest
        })
    }

    private fun fetchTags(): Set<MangaTag> {
        val response =
            webClient.httpGet("https://$domain/$apiSuffix/genres".toHttpUrl()).parseJsonArray()
        return response.mapJSONToSet { jo ->
            MangaTag(
                title = jo.getString("name").toTitleCase(sourceLocale),
                key = jo.getLong("id").toString(),
            )
        }
    }

    companion object {
        private const val LOCAL_PROXY = "http://127.0.0.1:8080/proxy"
        private var server: ProxyServer? = null

        private fun parseMetadata(meta: String): JSONObject {
            val out = JSONObject()
            val pos = JSONObject()
            val dims = JSONObject()
            var sw = 0
            var sh = 0

            for (t in meta.split("|")) {
                when {
                    t.startsWith("sw:") -> sw = t.substring(3).toInt()
                    t.startsWith("sh:") -> sh = t.substring(3).toInt()
                    t.contains("@") && t.contains(">") -> {
                        val (left, right) = t.split(">")
                        val (n, r) = left.split("@")
                        val (x, y, w, h) = r.split(",").map { it.toInt() }
                        dims.put(n, JSONObject().apply {
                            put("x", x); put("y", y); put("width", w); put("height", h)
                        })
                        pos.put(n, right)
                    }
                }
            }

            out.put("sw", sw)
            out.put("sh", sh)
            out.put("pos", pos)
            out.put("dims", dims)
            return out
        }

        fun decodeGt(hexData: String): String {
            val strategyStr = hexData.takeLast(2)
            val strategy = strategyStr.toInt(10)
            val encryptionKey = getFixedEncryptionKey(strategy)
            val encryptedHex = hexData.dropLast(2)
            val encryptedBytes = hexToBytes(encryptedHex)
            val keyBytes = encryptionKey.toByteArray(Charsets.UTF_8)
            val decrypted = ByteArray(encryptedBytes.size)

            for (i in encryptedBytes.indices) {
                decrypted[i] = (encryptedBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }

            return decrypted.toString(Charsets.UTF_8)
        }

        private fun getKeyByStrategy(strategy: Int): Double {
            return when (strategy) {
                0 -> 1.23872913102938
                1 -> 1.28767913123448
                2 -> 1.391378192300391
                3 -> 2.391378192500391
                4 -> 3.391378191230391
                5 -> 4.391373210965091
                6 -> 2.847291847392847
                7 -> 5.192847362847291
                8 -> 3.947382917483921
                9 -> 1.847392847291847
                10 -> 6.293847291847382
                11 -> 4.847291847392847
                12 -> 2.394827394827394
                13 -> 7.847291847392847
                14 -> 3.827394827394827
                15 -> 1.947382947382947
                16 -> 8.293847291847382
                17 -> 5.847291847392847
                18 -> 2.738472938472938
                19 -> 9.847291847392847
                20 -> 4.293847291847382
                21 -> 6.847291847392847
                22 -> 3.492847291847392
                23 -> 1.739482738472938
                24 -> 7.293847291847382
                25 -> 5.394827394827394
                26 -> 2.847391847392847
                27 -> 8.847291847392847
                28 -> 4.738472938472938
                29 -> 6.293847391847382
                30 -> 3.847291847392847
                31 -> 1.492847291847392
                32 -> 9.293847291847382
                33 -> 5.847291847392847
                34 -> 2.120381029475602
                35 -> 7.390481264726194
                36 -> 4.293012462419412
                37 -> 6.301412704170294
                38 -> 3.738472938472938
                39 -> 1.847291847392847
                40 -> 8.213901280149210
                41 -> 5.394827394827394
                42 -> 2.201381022038956
                43 -> 9.310129031284698
                44 -> 10.32131031284698
                45 -> 1.130712039820147
                else -> 1.2309829040349309
            }
        }

        private fun getFixedEncryptionKey(strategy: Int): String {
            val baseKey = getKeyByStrategy(strategy)
            return (PI * baseKey).toString()
        }

        private fun hexToBytes(hex: String): ByteArray {
            val bytes = ByteArray(hex.length / 2)
            for (i in hex.indices step 2) {
                bytes[i / 2] = hex.substring(i, i + 2).toInt(16).toByte()
            }
            return bytes
        }

        fun unscrambleImage(bitmap: Bitmap, drm: String): Bitmap {
            val decodedDrm = decodeGt(drm)
            val metadata = parseMetadata(decodedDrm)

            val sw = metadata.optInt("sw")
            val sh = metadata.optInt("sh")
            if (sw <= 0 || sh <= 0) return bitmap

            val fullW = bitmap.width
            val fullH = bitmap.height

            val working = Bitmap.createBitmap(bitmap, 0, 0, sw, sh)

            val keys = arrayOf("00","01","02","10","11","12","20","21","22")
            val wBase = sw / 3
            val hBase = sh / 3
            val rw = sw % 3
            val rh = sh % 3

            val defaultDims = HashMap<String, IntArray>().apply {
                for (k in keys) {
                    val i = k[0].digitToInt()
                    val j = k[1].digitToInt()
                    val w = wBase + if (j == 2) rw else 0
                    val h = hBase + if (i == 2) rh else 0
                    put(k, intArrayOf(j * wBase, i * hBase, w, h))
                }
            }

            val dimsJson = metadata.optJSONObject("dims") ?: JSONObject()
            val dims = HashMap<String, IntArray>().apply {
                for (k in keys) {
                    val jo = dimsJson.optJSONObject(k)
                    if (jo != null) {
                        put(k, intArrayOf(
                            jo.getInt("x"),
                            jo.getInt("y"),
                            jo.getInt("width"),
                            jo.getInt("height"),
                        ))
                    } else {
                        put(k, defaultDims.getValue(k))
                    }
                }
            }

            val pos = metadata.optJSONObject("pos") ?: JSONObject()
            val inv = HashMap<String, String>().apply {
                val it = pos.keys()
                while (it.hasNext()) {
                    val a = it.next()
                    val b = pos.getString(a)
                    put(b, a)
                }
            }

            val result = Bitmap.createBitmap(fullW, fullH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)

            for (k in keys) {
                val srcKey = inv[k] ?: continue
                val s = dims.getValue(k)
                val d = dims.getValue(srcKey)
                canvas.drawBitmap(
                    working,
                    Rect(s[0], s[1], s[0] + s[2], s[1] + s[3]),
                    Rect(d[0], d[1], d[0] + d[2], d[1] + d[3]),
                    null
                )
            }

            if (sh < fullH) {
                canvas.drawBitmap(bitmap, Rect(0, sh, fullW, fullH), Rect(0, sh, fullW, fullH), null)
            }
            if (sw < fullW) {
                canvas.drawBitmap(bitmap, Rect(sw, 0, fullW, sh), Rect(sw, 0, fullW, sh), null)
            }

            return result
        }
    }
}