package com.gitlab.mvysny.icloudphotopublicshare

import okhttp3.OkHttpClient
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL

/**
 * Reads a publicly shared iCloud album; no Apple ID needed:
 *
 * ```
 * val client: ICloudClient = ICloudClient.parseURL("https://www.icloud.com/sharedalbum/#B0AbCdEfGhIjKlM")
 * val photo: ICloudPhoto = client.getShare().photos[0]
 * val derivative: ICloudDerivative = photo.derivatives.values.maxByOrNull { it.height }!!
 * val imageUrl: URL = client.getDownloadUrl(photo, derivative)   // valid for about an hour
 * ```
 *
 * Each `get*` call blocks on an HTTP request, so keep them off Android's main thread. They throw [IOException] on a
 * network failure, a non-2xx or a malformed response, [FileNotFoundException] for 404.
 *
 * Thread-safe.
 *
 * @property albumId such as `B0AbCdEfGhIjKlM` in `https://www.icloud.com/sharedalbum/#B0AbCdEfGhIjKlM`; [parseURL]
 *   extracts it.
 * @param serverName the iCloud server holding the album. A wrong one costs one extra request: iCloud redirects, and the
 *   client remembers the right server.
 * @param httpClient carries every request; pass your own to configure timeouts, a proxy or a cache. By default all
 *   clients share one.
 * @throws IllegalArgumentException if [albumId] is not letters and digits, or [serverName] is not a
 *   `pNN-sharedstreams.icloud.com` host.
 */
public class ICloudClient(
        public val albumId: String,
        @Volatile
        private var serverName: String = "p27-sharedstreams.icloud.com",
        private val httpClient: OkHttpClient = sharedHttpClient
) {
    init {
        require(albumIdRegex.matches(albumId)) { "Invalid album ID '$albumId': expected letters and digits" }
        require(serverNameRegex.matches(serverName)) {
            "Invalid server name '$serverName': expected a pNN-sharedstreams.icloud.com host"
        }
    }

    private val url: String get() = "https://$serverName/$albumId/sharedstreams"

    /**
     * POSTs [body] as JSON to this album's [endpoint] and maps the response to [clazz], following iCloud's 330
     * redirects to the server that holds the album.
     */
    private fun <T> post(endpoint: String, body: Map<String, Any?>, clazz: Class<T>): T {
        repeat(MAX_REDIRECTS) {
            httpClient.postJson("$url/$endpoint", body).use { response ->
                if (response.code != 330) return response.checkOk().json(clazz)
                val host: String = required(response.json(RedirectJson::class.java).host, "X-Apple-MMe-Host", "$url/$endpoint")
                if (!serverNameRegex.matches(host)) throw IOException("330: redirect to unexpected host $host ($url/$endpoint)")
                serverName = host
            }
        }
        throw IOException("330: more than $MAX_REDIRECTS redirects ($url/$endpoint)")
    }

    /**
     * Fetches the album's name, owner and photo list.
     */
    public fun getShare(): ICloudShare =
        post("webstream", mapOf("streamCtag" to null), ShareJson::class.java).toShare("$url/webstream")

    /**
     * Returns a download URL for [derivative] of [photo]; it expires in about an hour. Costs one request per call, so
     * for several derivatives of one photo fetch [getWebAsset] once instead.
     * @throws IllegalStateException if iCloud has no such derivative of [photo].
     */
    public fun getDownloadUrl(photo: ICloudPhoto, derivative: ICloudDerivative): URL =
            getDownloadUrl(photo.photoGuid, derivative.checksum)

    /**
     * Fetches the download URLs of every derivative of [photo] in one request; pick one with [WebAsset.getDownloadUrl].
     */
    public fun getWebAsset(photo: ICloudPhoto): WebAsset = getWebAsset(photo.photoGuid)

    /**
     * Same as the [ICloudPhoto] overload, by [ICloudPhoto.photoGuid].
     */
    public fun getWebAsset(photoGuid: String): WebAsset =
        post("webasseturls", mapOf("photoGuids" to listOf(photoGuid)), WebAssetJson::class.java).toWebAsset("$url/webasseturls")

    /**
     * Same as the [ICloudPhoto] overload, by [ICloudPhoto.photoGuid] and [ICloudDerivative.checksum].
     * @throws IllegalStateException if iCloud has no such derivative of the photo.
     */
    public fun getDownloadUrl(photoGuid: String, checksum: String): URL {
        val webAsset: WebAsset = getWebAsset(photoGuid)
        val url: URL = webAsset.getDownloadUrl(checksum)
        return url
    }

    public companion object {
        private const val MAX_REDIRECTS = 5
        private val sharedHttpClient: OkHttpClient by lazy { OkHttpClient() }
        private val albumIdRegex = Regex("[A-Za-z0-9]+")
        private val serverNameRegex = Regex("p\\d+-sharedstreams\\.icloud\\.com")
        private val sharedStreamsUrlRegex = Regex("https://(${serverNameRegex.pattern})/(${albumIdRegex.pattern})/sharedstreams.*")
        private val sharedAlbumUrlRegex = Regex("https://www\\.icloud\\.com/sharedalbum/#(${albumIdRegex.pattern})")

        /**
         * Creates a client for an album URL such as `https://www.icloud.com/sharedalbum/#B0AbCdEfGhIjKlM` or
         * `https://p27-sharedstreams.icloud.com/B0AbCdEfGhIjKlM/sharedstreams`.
         * @param httpClient see [ICloudClient].
         * @throws IllegalArgumentException if [url] is neither form.
         */
        public fun parseURL(url: String, httpClient: OkHttpClient = sharedHttpClient): ICloudClient {
            sharedAlbumUrlRegex.matchEntire(url)?.let { return ICloudClient(it.groupValues[1], httpClient = httpClient) }
            val match: MatchResult = sharedStreamsUrlRegex.matchEntire(url)
                ?: throw IllegalArgumentException("Failed to parse $url: not an iCloud shared album URL")
            return ICloudClient(match.groupValues[2], match.groupValues[1], httpClient)
        }
    }

    override fun toString(): String = "ICloudClient(albumId='$albumId', serverName='$serverName')"
}
