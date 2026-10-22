package com.github.mvysny.icloudphotopublicshare

import com.google.gson.annotations.SerializedName
import java.io.IOException

// The shapes of iCloud's JSON, as Gson maps them; the public model is built from these by the `to*` functions, which
// fail on a missing id.
//
// Implementation notes: Gson writes fields by reflection, so mapping straight into the read-only public model would
// mutate final fields, which JDK 26 warns about (JEP 500) and a later JDK refuses. Hence `var`s here, `val`s there.
// The `Json` suffix is load-bearing: the shipped R8/ProGuard rule (META-INF/proguard) keeps these classes by it.

internal class ShareJson(
    var userFirstName: String? = null,
    var userLastName: String? = null,
    var streamName: String? = null,
    var streamCtag: String? = null,
    var itemsReturned: Int = 0,
    var photos: List<PhotoJson>? = null
)

internal class PhotoJson(
    var batchGuid: String? = null,
    var derivatives: Map<String, DerivativeJson>? = null,
    var contributorFirstName: String? = null,
    var contributorLastName: String? = null,
    var photoGuid: String? = null,
    var batchDateCreated: String? = null,
    var dateCreated: String? = null,
    var width: Int = 0,
    var height: Int = 0,
    var caption: String? = null
)

internal class DerivativeJson(
    var fileSize: Long = 0,
    var checksum: String? = null,
    var width: Int = 0,
    var height: Int = 0
)

internal class WebAssetJson(
    var locations: Map<String, LocationJson>? = null,
    var items: Map<String, WebItemJson>? = null
)

internal class LocationJson(
    var scheme: String? = null,
    var hosts: List<String>? = null
)

internal class WebItemJson(
    var url_expiry: String? = null,
    var url_location: String? = null,
    var url_path: String? = null
)

/**
 * The body of iCloud's 330 response: the album lives on another server.
 * @property host that server's host name, such as `p27-sharedstreams.icloud.com`.
 */
internal class RedirectJson(
    @SerializedName("X-Apple-MMe-Host")
    var host: String? = null
)

/**
 * @param url the request URL, for the error message.
 * @throws IOException if [value] is null or blank.
 */
internal fun required(value: String?, name: String, url: String): String {
    if (value.isNullOrBlank()) throw IOException("Malformed iCloud response, missing $name ($url)")
    return value
}

internal fun ShareJson.toShare(url: String): ICloudShare = ICloudShare(
    userFirstName = userFirstName,
    userLastName = userLastName,
    streamName = streamName,
    streamCtag = streamCtag,
    itemsReturned = itemsReturned,
    photos = photos.orEmpty().map { it.toPhoto(url) }
)

private fun PhotoJson.toPhoto(url: String): ICloudPhoto = ICloudPhoto(
    batchGuid = batchGuid,
    derivatives = derivatives.orEmpty().mapValues { (_, derivative) -> derivative.toDerivative(url) },
    contributorFirstName = contributorFirstName,
    contributorLastName = contributorLastName,
    photoGuid = required(photoGuid, "photoGuid", url),
    batchDateCreated = batchDateCreated,
    dateCreated = dateCreated,
    width = width,
    height = height,
    caption = caption
)

private fun DerivativeJson.toDerivative(url: String): ICloudDerivative =
    ICloudDerivative(fileSize, required(checksum, "checksum", url), width, height)

internal fun WebAssetJson.toWebAsset(url: String): WebAsset = WebAsset(
    locations = locations.orEmpty().mapValues { (_, location) -> ILocation(location.scheme, location.hosts.orEmpty()) },
    items = items.orEmpty().mapValues { (_, item) ->
        WebItem(item.url_expiry, required(item.url_location, "url_location", url), required(item.url_path, "url_path", url))
    }
)
