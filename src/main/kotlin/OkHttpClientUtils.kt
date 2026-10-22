package com.github.mvysny.icloudphotopublicshare

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.FileNotFoundException
import java.io.IOException

private val mimeJson: MediaType = "application/json".toMediaType()
private val gson: Gson = GsonBuilder().serializeNulls().create()

/**
 * POSTs [body] to [url], serialized to JSON with nulls kept; the caller closes the response.
 */
internal fun OkHttpClient.postJson(url: String, body: Any): Response {
    val request: Request = Request.Builder().url(url).post(gson.toJson(body).toRequestBody(mimeJson)).build()
    return newCall(request).execute()
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
 * Maps the body to [clazz], one of the `*Json` classes.
 */
internal fun <T> Response.json(clazz: Class<T>): T = body!!.use { gson.fromJson(it.charStream(), clazz) }
