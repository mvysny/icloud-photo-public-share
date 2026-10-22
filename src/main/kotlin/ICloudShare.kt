package com.gitlab.mvysny.icloudphotopublicshare

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * A shared album, as [ICloudClient.getShare] returns it.
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
 * One photo of an [ICloudShare]: its metadata and the sizes iCloud serves it in.
 * @property derivatives the available sizes, keyed by the longer side in pixels, such as `"1024"`.
 * @property photoGuid identifies the photo, in UUID format.
 * @property batchDateCreated ISO 8601 instant such as `2020-10-20T07:19:44Z`.
 * @property dateCreated ISO 8601 instant such as `2019-10-25T05:26:58Z`.
 * @property width in pixels.
 * @property height in pixels.
 * @property caption usually blank.
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
 * One size of an [ICloudPhoto]; [ICloudClient.getDownloadUrl] turns it into a download URL.
 * @property fileSize in bytes.
 * @property checksum identifies this derivative; the key into [WebAsset.items].
 * @property width in pixels.
 * @property height in pixels.
 */
public data class ICloudDerivative(
        var fileSize: Long = 0,
        var checksum: String? = null,
        var width: Int = 0,
        var height: Int = 0
) : Serializable

/**
 * The body of iCloud's 330 response: the album lives on another server.
 * @property xapplemmehost that server's host name, such as `p27-sharedstreams.icloud.com`.
 */
internal data class ICloudRedirect(
        @SerializedName("X-Apple-MMe-Host")
        var xapplemmehost: String? = null
)
