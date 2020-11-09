package com.gitlab.mvysny.icloudphotopublicshare

import okhttp3.OkHttpClient

public fun main() {
    ICloud.okHttpClient = OkHttpClient()

    println(ICloudClient.parseURL("https://p23-sharedstreams.icloud.com/B0R5qXGF1GeKg4d/sharedstreams/webstream").getShare())
}
