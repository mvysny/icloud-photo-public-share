package com.gitlab.mvysny.icloudphotopublicshare

import java.net.URL

/**
 * @property locations maps [WebItem.url_location] to [ILocation]
 * @property items maps [ICloudDerivative.checksum] to [WebItem]
 */
public data class WebAsset(
        public val locations: MutableMap<String, ILocation> = mutableMapOf(),
        public val items: MutableMap<String, WebItem> = mutableMapOf()
) {

    public fun getDownloadUrl(derivative: ICloudDerivative): URL = getDownloadUrl(derivative.checksum!!)

    public fun getDownloadUrl(checksum: String): URL {
        val item: WebItem = checkNotNull(items[checksum]) {
            "No item with checksum '$checksum'. Available items: ${items.values.toList()}"
        }
        val location: ILocation? = locations[item.url_location!!]
        val scheme: String = location?.scheme ?: "https"
        val hostName: String = location?.hosts?.get(0) ?: item.url_location!!
        val url = "$scheme://$hostName${item.url_path}"
        return URL(url)
    }
}

public data class ILocation(
        var scheme: String? = null,
        val hosts: MutableList<String> = mutableListOf()
)

public data class WebItem(
        var url_expiry: String? = null,
        var url_location: String? = null,
        var url_path: String? = null
)
