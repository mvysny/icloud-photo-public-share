package com.github.mvysny.icloudphotopublicshare

import java.io.Serializable

/**
 * A shared album, as [ICloudClient.getShare] returns it.
 */
public data class ICloudShare(
        val userFirstName: String?,
        val userLastName: String?,
        val streamName: String?,
        val streamCtag: String?,
        val itemsReturned: Int,
        val photos: List<ICloudPhoto>
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
        val batchGuid: String?,
        val derivatives: Map<String, ICloudDerivative>,
        val contributorFirstName: String?,
        val contributorLastName: String?,
        val photoGuid: String,
        val batchDateCreated: String?,
        val dateCreated: String?,
        val width: Int,
        val height: Int,
        val caption: String?
) : Serializable {
    /**
     * The derivative with the longest longer side, usually the full-size photo.
     * @throws IllegalStateException if this photo has no derivatives.
     */
    public val largestDerivative: ICloudDerivative
        get() = checkNotNull(derivatives.values.maxByOrNull { it.longerSide }) { "Photo $photoGuid has no derivatives" }

    /**
     * Returns the smallest derivative whose longer side is at least [px] pixels; null if all are smaller:
     *
     * ```
     * val thumbnail: ICloudDerivative = photo.derivativeAtLeast(400) ?: photo.largestDerivative
     * ```
     */
    public fun derivativeAtLeast(px: Int): ICloudDerivative? =
        derivatives.values.filter { it.longerSide >= px }.minByOrNull { it.longerSide }
}

/**
 * One size of an [ICloudPhoto]; [ICloudClient.getDownloadUrl] turns it into a download URL.
 * @property fileSize in bytes.
 * @property checksum identifies this derivative; the key into [WebAsset.items].
 * @property width in pixels.
 * @property height in pixels.
 */
public data class ICloudDerivative(
        val fileSize: Long,
        val checksum: String,
        val width: Int,
        val height: Int
) : Serializable

private val ICloudDerivative.longerSide: Int get() = maxOf(width, height)
