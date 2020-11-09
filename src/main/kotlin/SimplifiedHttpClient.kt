package com.gitlab.mvysny.icloudphotopublicshare

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.FileNotFoundException
import java.io.IOException

public interface SimplifiedHttpClient {
    public fun <T> post(url: String, post: String, responseHandler: (HttpResponse) -> T): T
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

public class SimplifiedOkHttpClient(public val client: OkHttpClient) : SimplifiedHttpClient {

    override fun <T> post(url: String, postJson: String, responseHandler: (HttpResponse) -> T): T {
        val request: Request = url.toHttpUrl().buildRequest {
            post(RequestBody.create(mimeJson, postJson))
        }

        val result: T = client.newCall(request).execute().use { response: Response ->
            responseHandler(OkHttpResponse(url, response))
        }

        return result
    }
}

public class OkHttpResponse(public override val url: String, public val response: Response) : HttpResponse {
    override val code: Int
        get() = response.code()

    override fun body(): String = response.body()!!.use { it.string() }

    override fun <T> json(clazz: Class<T>): T = response.body()!!.use { it.json(clazz) }
}
