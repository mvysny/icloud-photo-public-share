package com.gitlab.mvysny.icloudphotopublicshare

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Information about a particular iCloud share. See [photos] for a list of photos.
 */
public data class ICloudShare(
        var userFirstName: String? = null,
        var userLastName: String? = null,
        var streamName: String? = null,
        var streamCtag: String? = null,
        var itemsReturned: Int = 0,
        var photos: MutableList<ICloudPhoto> = mutableListOf()
) : Serializable

/**
 * Meta-data about a particular iCloud photo.
 * @property batchGuid ?
 * @property derivatives lists all available sizes of the photo.
 * @property contributorFirstName
 * @property contributorLastName
 * @property photoGuid unique identifier of the photo, in the UUID formatting.
 * @property batchDateCreated ISO date such as `"2020-10-20T07:19:44Z"`
 * @property dateCreated ISO date such as `"2020-10-20T07:19:44Z"`.
 * @property width (original?) photo width
 * @property height (original?) photo height
 * @property caption usually blank
 */
public data class ICloudPhoto(
        var batchGuid: String? = null,
        var derivatives: MutableMap<String, ICloudDerivative> = mutableMapOf(),
        var contributorFirstName: String? = null,
        var contributorLastName: String? = null,
        var photoGuid: String? = null,
        var batchDateCreated: String? = null,
        var dateCreated: String? = null,
        var width: Int = 0,
        var height: Int = 0,
        var caption: String? = null
) : Serializable

/**
 * Information on a particular resized version of a photo. The [checksum] string
 * is an unique identifier and is important when obtaining the downloadable URL.
 * @property fileSize jpg file size, in bytes
 * @property checksum derivative ID
 * @property width width in pixels
 * @property height height in pixels
 */
public data class ICloudDerivative(
        var fileSize: Long = 0,
        var checksum: String? = null,
        var width: Int = 0,
        var height: Int = 0
) : Serializable

/**
 * Sent by iCloud when the photo resides on a different server.
 */
internal data class ICloudRedirect(
        @SerializedName("X-Apple-MMe-Host")
        var xapplemmehost: String? = null
)
