package com.gitlab.mvysny.icloudphotopublicshare

import okhttp3.OkHttpClient
import java.net.URL

public fun main() {
    ICloud.httpClient = SimplifiedOkHttpClient(OkHttpClient())
    val client: ICloudClient = ICloudClient.parseURL("https://p27-sharedstreams.icloud.com/B0R5qXGF1GeKg4d/sharedstreams")
    val share: ICloudShare = client.getShare()
    val photo: ICloudPhoto = share.photos[0]
    val derivative: ICloudDerivative = photo.derivatives.values.maxByOrNull { it.height }!!
    val imageUrl: URL = client.getDownloadUrl(photo, derivative)
    println(imageUrl)
}
