package com.gitlab.mvysny.icloudphotopublicshare

import java.net.URL

/**
 * Demo: prints a download URL for the first photo of a live album.
 */
public fun main() {
    val client: ICloudClient = ICloudClient.parseURL("https://www.icloud.com/sharedalbum/#B0R5qXGF1GeKg4d")
    val share: ICloudShare = client.getShare()

    // download first photo
    val photo: ICloudPhoto = share.photos[0]
    val derivative: ICloudDerivative = photo.derivatives.values.maxByOrNull { it.height }!!

    // the URL is only valid for an hour or so
    val imageUrl: URL = client.getDownloadUrl(photo, derivative)
    println(imageUrl)
}
