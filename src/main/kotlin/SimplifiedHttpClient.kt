package com.gitlab.mvysny.icloudphotopublicshare

import java.io.FileNotFoundException
import java.io.IOException

public interface SimplifiedHttpClient {
    public fun <T> post(url: String, postJson: String, responseHandler: (HttpResponse) -> T): T
}

public interface HttpResponse {
    public val url: String
    public val code: Int

    public val isSuccessful: Boolean get() = code in 200..299

    public fun checkOk() {
        if (!isSuccessful) {
            val msg = "${code}: ${body()} ($url)"
            if (code == 404) throw FileNotFoundException(msg)
            throw IOException(msg)
        }
    }

    public fun body(): String
    public fun <T> json(clazz: Class<T>): T
}
