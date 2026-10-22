package com.gitlab.mvysny.icloudphotopublicshare

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL

/**
 * Global configuration for [ICloudClient]. Configure it before the first request: the fields are read without
 * synchronization.
 */
public object ICloud {
    /**
     * Every iCloud request goes through this client; replace it to configure timeouts, a proxy or a cache.
     */
    public var httpClient: OkHttpClient = OkHttpClient()

    /**
     * Maps iCloud's JSON responses to [ICloudShare], [WebAsset] and the rest of the model.
     */
    public var gson: Gson = GsonBuilder().create()
}

/**
 * Reads a publicly shared iCloud album; no Apple ID needed:
 *
 * ```
 * val client: ICloudClient = ICloudClient.parseURL("https://www.icloud.com/sharedalbum/#B0R5qXGF1GeKg4d")
 * val photo: ICloudPhoto = client.getShare().photos[0]
 * val derivative: ICloudDerivative = photo.derivatives.values.maxByOrNull { it.height }!!
 * val imageUrl: URL = client.getDownloadUrl(photo, derivative)   // valid for about an hour
 * ```
 *
 * Each `get*` call blocks on an HTTP request through [ICloud.httpClient], so keep them off Android's main thread. They
 * throw [IOException] on a network failure or a non-2xx response, [FileNotFoundException] for 404.
 *
 * Thread-safe.
 *
 * @property albumId such as `B0R5qXGF1GeKg4d` in `https://www.icloud.com/sharedalbum/#B0R5qXGF1GeKg4d`; [parseURL]
 *   extracts it.
 * @param serverName the iCloud server holding the album. A wrong one costs [getShare] an extra request (it follows
 *   iCloud's redirect and remembers the right server) but fails the other `get*` calls until [getShare] has run.
 */
public class ICloudClient(
        public val albumId: String,
        @Volatile
        private var serverName: String = "p27-sharedstreams.icloud.com"
) {

    private val url: String get() = "https://$serverName/$albumId/sharedstreams"

    /**
     * Fetches the album's name, owner and photo list, following iCloud's redirect to the server that holds it.
     */
    public fun getShare(): ICloudShare {
        while (true) {
            val share: ICloudShare? = ICloud.httpClient.postJson("$url/webstream", "{\"streamCtag\":null}") { response ->
                if (response.code == 330) {
                    // redirect to another server
                    val redirect: ICloudRedirect = response.json(ICloudRedirect::class.java)
                    serverName = redirect.xapplemmehost!!
                    null
                } else {
                    response.checkOk().json(ICloudShare::class.java)
                }
            }

            if (share != null) {
                return share
            }
        }
    }

    /**
     * Returns a download URL for [derivative] of [photo]; it expires in about an hour. Costs one request per call, so
     * for several derivatives of one photo fetch [getWebAsset] once instead.
     * @throws IllegalStateException if iCloud has no such derivative of [photo].
     */
    public fun getDownloadUrl(photo: ICloudPhoto, derivative: ICloudDerivative): URL =
            getDownloadUrl(photo.photoGuid!!, derivative.checksum!!)

    /**
     * Fetches the download URLs of every derivative of [photo] in one request; pick one with [WebAsset.getDownloadUrl].
     */
    public fun getWebAsset(photo: ICloudPhoto): WebAsset = getWebAsset(photo.photoGuid!!)

    /**
     * Same as the [ICloudPhoto] overload, by [ICloudPhoto.photoGuid].
     */
    public fun getWebAsset(photoGuid: String): WebAsset =
        ICloud.httpClient.postJson("$url/webasseturls", "{\"photoGuids\": [\"$photoGuid\"]}") { response ->
            response.checkOk().json(WebAsset::class.java)
        }

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
        private val urlRegex = Regex("https://(p\\d+-sharedstreams.icloud.com)/([A-Za-z0-9]+)/sharedstreams.*")
        /**
         * Creates a client for an album URL such as `https://www.icloud.com/sharedalbum/#B0R5qXGF1GeKg4d` or
         * `https://p27-sharedstreams.icloud.com/B0R5qXGF1GeKg4d/sharedstreams`.
         * @throws IllegalArgumentException if [url] is neither form.
         */
        public fun parseURL(url: String): ICloudClient {
            if (url.startsWith("https://www.icloud.com/sharedalbum/#")) {
                return ICloudClient(url.removePrefix("https://www.icloud.com/sharedalbum/#"))
            }
            val match: MatchResult = urlRegex.matchEntire(url) ?: throw IllegalArgumentException("Failed to parse $url: not a sharedstreams.icloud.com URL")
            return ICloudClient(match.groupValues[2], match.groupValues[1])
        }
    }

    override fun toString(): String = "ICloudClient(albumId='$albumId', serverName='$serverName')"
}
