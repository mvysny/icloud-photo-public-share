package com.gitlab.mvysny.icloudphotopublicshare

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.MediaType
import java.io.InputStream
import java.net.URL

public object ICloud {
    /**
     * All REST client calls will reuse this client.
     */
    public var httpClient: SimplifiedHttpClient? = null

    /**
     * The default [Gson] interface used by all serialization/deserialization methods. Simply reassign with another [Gson]
     * instance to reconfigure. To be thread-safe, do the reassignment in your `ServletContextListener`.
     */
    public var gson: Gson = GsonBuilder().create()
}

/**
 * @property albumId the album ID, something like `B0R5qXGF1GeKg4d`. Usually a part of the shared photo album URL
 * such as `https://p27-sharedstreams.icloud.com/B0R5qXGF1GeKg4d/sharedstreams/webstream`.
 * Use [parseURL] to parse such URLs.
 */
public class ICloudClient(public val albumId: String, private var serverName: String = "p27-sharedstreams.icloud.com") {

    private val url: String get() = "https://$serverName/$albumId/sharedstreams"

    public fun getShare(): ICloudShare {
        while (true) {
            val share: ICloudShare? = ICloud.httpClient!!.post("$url/webstream", "{\"streamCtag\":null}") { response ->
                if (response.code == 330) {
                    // redirect to another server
                    val redirect: ICloudRedirect = response.json(ICloudRedirect::class.java)
                    serverName = redirect.xapplemmehost!!
                    null
                } else {
                    response.checkOk()
                    response.json(ICloudShare::class.java)
                }
            }

            if (share != null) {
                return share
            }
        }
    }

    public fun download(photoGuid: String, derivative: ICloudDerivative): InputStream =
            download(photoGuid, derivative.checksum!!)

    public fun getWebAsset(photo: ICloudPhoto): WebAsset = getWebAsset(photo.photoGuid!!)

    public fun getWebAsset(photoGuid: String): WebAsset =
        ICloud.httpClient!!.post("$url/webasseturls", "{\"photoGuids\": [\"$photoGuid\"]}") { response: HttpResponse ->
            response.checkOk()
            response.json(WebAsset::class.java)
        }

    public fun download(photoGuid: String, checksum: String): InputStream {
        val webAsset: WebAsset = getWebAsset(photoGuid)
        val url: URL = webAsset.getDownloadUrl(checksum)
        return url.openStream()
    }

    public companion object {
        private val urlRegex = Regex("https://(p\\d+-sharedstreams.icloud.com)/([A-Za-z0-9]+)/sharedstreams.*")
        /**
         * Creates the client instance for URLs such as `https://p27-sharedstreams.icloud.com/B0R5qXGF1GeKg4d/sharedstreams`.
         */
        public fun parseURL(url: String): ICloudClient {
            val match: MatchResult = urlRegex.matchEntire(url) ?: throw IllegalArgumentException("Failed to parse $url: not a sharedstreams.icloud.com URL")
            return ICloudClient(match.groupValues[2], match.groupValues[1])
        }
    }

    override fun toString(): String = "ICloudClient(albumId='$albumId', serverName='$serverName')"
}

internal val mimeJson: MediaType = MediaType.get("application/json")
