package com.gitlab.mvysny.icloudphotopublicshare

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import java.net.URL

/**
 * Global configuration for [ICloudClient].
 */
public object ICloud {
    /**
     * Every iCloud request goes through this client; replace it to configure timeouts, a proxy or a cache.
     */
    public var httpClient: OkHttpClient = OkHttpClient()

    /**
     * The default [Gson] interface used by all serialization/deserialization methods. Simply reassign with another [Gson]
     * instance to reconfigure. To be thread-safe, do the reassignment in your `ServletContextListener`.
     */
    public var gson: Gson = GsonBuilder().create()
}

/**
 * A client for icloud.com. No authentication needed: "anyone with the link can view".
 *
 * To use:
 *
 * * Call [getShare] to retrieve the list of all shared photos;
 * * Pick the appropriate photo size from [ICloudPhoto.derivatives]
 * * Call [getDownloadUrl] to obtain the download URL from the [ICloudPhoto] and [ICloudDerivative] pair.
 * * The download link is usually valid for one hour.
 *
 * @property albumId the album ID, something like `B0R5qXGF1GeKg4d`. Usually a part of the shared photo album URL
 * such as `https://p27-sharedstreams.icloud.com/B0R5qXGF1GeKg4d/sharedstreams/webstream`.
 * Use [parseURL] to parse such URLs.
 * @property serverName the host name of the server to communicate with, usually in the form of `p27-sharedstreams.icloud.com`.
 * Modified when icloud.com responds with a redirect request.
 */
public class ICloudClient(
        public val albumId: String,
        @Volatile
        private var serverName: String = "p27-sharedstreams.icloud.com"
) {

    private val url: String get() = "https://$serverName/$albumId/sharedstreams"

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
     * Obtains the URL for given [photo] and [derivative],
     * from which a jpg image can be downloaded. The link is usually
     * valid for an hour.
     */
    public fun getDownloadUrl(photo: ICloudPhoto, derivative: ICloudDerivative): URL =
            getDownloadUrl(photo.photoGuid!!, derivative.checksum!!)

    /**
     * Obtains [WebAsset] for given [photo].
     */
    public fun getWebAsset(photo: ICloudPhoto): WebAsset = getWebAsset(photo.photoGuid!!)

    public fun getWebAsset(photoGuid: String): WebAsset =
        ICloud.httpClient.postJson("$url/webasseturls", "{\"photoGuids\": [\"$photoGuid\"]}") { response ->
            response.checkOk().json(WebAsset::class.java)
        }

    /**
     * Obtains the URL from which a jpg image can be downloaded. The link is usually
     * valid for an hour.
     */
    public fun getDownloadUrl(photoGuid: String, checksum: String): URL {
        val webAsset: WebAsset = getWebAsset(photoGuid)
        val url: URL = webAsset.getDownloadUrl(checksum)
        return url
    }

    public companion object {
        private val urlRegex = Regex("https://(p\\d+-sharedstreams.icloud.com)/([A-Za-z0-9]+)/sharedstreams.*")
        /**
         * Creates the client instance for URLs such as `https://p27-sharedstreams.icloud.com/B0R5qXGF1GeKg4d/sharedstreams`
         * or `https://www.icloud.com/sharedalbum/#B0R5qXGF1GeKg4d`.
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
