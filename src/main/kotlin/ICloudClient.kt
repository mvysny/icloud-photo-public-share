package com.gitlab.mvysny.icloudphotopublicshare

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType
import java.io.Serializable

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
            val share: ICloudShare? = ICloud.httpClient!!.post("${this.url}/webstream", "{\"streamCtag\":null}") { response ->
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

internal val mimeJson = MediaType.get("application/json")

public data class ICloudShare(
        var userFirstName: String? = null,
        var userLastName: String? = null,
        var streamName: String? = null,
        var streamCtag: String? = null,
        var itemsReturned: Int = 0,
        var photos: MutableList<ICloudPhoto> = mutableListOf()
) : Serializable

public data class ICloudPhoto(
        var batchGuid: String? = null,
        var derivatives: MutableMap<String, ICloudDerivative> = mutableMapOf(),
        var contributorFirstName: String? = null,
        var contributorLastName: String? = null,
        var photoGuid: String? = null,
        /**
         * ISO date such as `"2020-10-20T07:19:44Z"`
         */
        var batchDateCreated: String? = null,
        /**
         * ISO date such as `"2020-10-20T07:19:44Z"`.
         */
        var dateCreated: String? = null,
        var width: Int = 0,
        var height: Int = 0,
        var caption: String? = null
) : Serializable

public data class ICloudDerivative(
        var fileSize: Long = 0,
        var checksum: String? = null,
        var width: Int = 0,
        var height: Int = 0
) : Serializable

internal data class ICloudRedirect(
        @SerializedName("X-Apple-MMe-Host")
        var xapplemmehost: String? = null
)
