package com.gitlab.mvysny.icloudphotopublicshare

import okhttp3.OkHttpClient

public fun main() {
    ICloud.httpClient = SimplifiedOkHttpClient(OkHttpClient())
    val share = ICloudClient.parseURL("https://p27-sharedstreams.icloud.com/B0R5qXGF1GeKg4d/sharedstreams/webstream").getShare()
    println(share)
}
