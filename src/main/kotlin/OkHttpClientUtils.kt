package com.gitlab.mvysny.icloudphotopublicshare

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.FileNotFoundException
import java.io.IOException

private val mimeJson: MediaType = "application/json".toMediaType()

/**
 * POSTs [json] to [url] and hands the response to [handler], closing it afterwards.
 */
internal fun <T> OkHttpClient.postJson(url: String, json: String, handler: (Response) -> T): T {
    val request: Request = Request.Builder().url(url).post(json.toRequestBody(mimeJson)).build()
    return newCall(request).execute().use(handler)
}

/**
 * Returns this response if its code is 2xx.
 * @throws IOException carrying the code, body and URL otherwise; [FileNotFoundException] for 404.
 */
internal fun Response.checkOk(): Response {
    if (!isSuccessful) {
        val msg = "${code}: ${body!!.string()} (${request.url})"
        if (code == 404) throw FileNotFoundException(msg)
        throw IOException(msg)
    }
    return this
}

/**
 * Maps the body to [clazz] with [ICloud.gson].
 */
internal fun <T> Response.json(clazz: Class<T>): T = body!!.use { ICloud.gson.fromJson(it.charStream(), clazz) }
