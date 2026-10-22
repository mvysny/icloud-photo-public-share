package com.github.mvysny.icloudphotopublicshare

import java.net.URL

/**
 * The download URLs of one photo's derivatives, as [ICloudClient.getWebAsset] returns them.
 * @property locations maps [WebItem.url_location] to the server serving it.
 * @property items maps [ICloudDerivative.checksum] to where that derivative downloads from.
 */
public data class WebAsset(
        public val locations: Map<String, ILocation>,
        public val items: Map<String, WebItem>
) {

    /**
     * Returns the download URL of [derivative]; it expires at [WebItem.url_expiry], about an hour after the fetch.
     * @throws IllegalStateException if this asset has no such derivative, say one of another photo.
     */
    public fun getDownloadUrl(derivative: ICloudDerivative): URL = getDownloadUrl(derivative.checksum)

    /**
     * Same as the [ICloudDerivative] overload, by [ICloudDerivative.checksum].
     * @throws IllegalStateException if this asset has no such derivative.
     */
    public fun getDownloadUrl(checksum: String): URL {
        val item: WebItem = checkNotNull(items[checksum]) {
            "No item with checksum '$checksum'. Available items: ${items.values.toList()}"
        }
        val location: ILocation? = locations[item.url_location]
        val scheme: String = location?.scheme ?: "https"
        val hostName: String = location?.hosts?.firstOrNull() ?: item.url_location
        return URL("$scheme://$hostName${item.url_path}")
    }
}

/**
 * A server a [WebItem] downloads from.
 * @property scheme null means `https`.
 * @property hosts usually just one; [WebAsset.getDownloadUrl] uses the first.
 */
public data class ILocation(
        val scheme: String?,
        val hosts: List<String>
)

/**
 * Where one derivative downloads from.
 * @property url_expiry ISO 8601 instant when the URL expires.
 * @property url_location the key into [WebAsset.locations]; taken as the host name itself when no location provides
 *   one.
 * @property url_path the path and query of the download URL.
 */
public data class WebItem(
        val url_expiry: String?,
        val url_location: String,
        val url_path: String
)
