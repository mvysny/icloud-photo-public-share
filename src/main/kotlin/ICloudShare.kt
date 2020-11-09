package com.gitlab.mvysny.icloudphotopublicshare

import com.google.gson.annotations.SerializedName
import java.io.Serializable

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
