package com.gitlab.mvysny.icloudphotopublicshare

import okhttp3.OkHttpClient

public fun main() {
    ICloud.httpClient = SimplifiedOkHttpClient(OkHttpClient())
    val client: ICloudClient = ICloudClient.parseURL("https://p27-sharedstreams.icloud.com/B0R5qXGF1GeKg4d/sharedstreams/webstream")
    val share: ICloudShare = client.getShare()
    val webAsset = client.getWebAsset(share.photos[0])
    println(webAsset)
    println(webAsset.getDownloadUrl(share.photos[0].derivatives.values.first()))
}
