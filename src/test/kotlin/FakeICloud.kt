package com.gitlab.mvysny.icloudphotopublicshare

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.io.InputStream

/**
 * Plays iCloud on a local [MockWebServer], replaying the recorded responses in `src/test/resources`:
 *
 * ```
 * FakeICloud().use { fake ->
 *     ICloud.httpClient = fake.client
 *     ICloudClient("B0R5qXGF1GeKg4d").getShare()
 * }
 * ```
 *
 * Knows album `B0R5qXGF1GeKg4d` on `p27-sharedstreams.icloud.com`; every other `pNN` host answers 330, redirecting there.
 *
 * ## Implementation notes
 *
 * [client] reroutes each `https://` iCloud request to the server but keeps the iCloud host in the `Host` header, so the
 * dispatcher routes on it like a real virtual host. A request that fails a check gets a 400 rather than an assertion
 * error, since the dispatcher runs on the server's thread and the test would only see a dropped connection.
 */
class FakeICloud : AutoCloseable {
    private val server = MockWebServer().apply {
        dispatcher = ICloudDispatcher()
        start()
    }

    /**
     * Sends every request to the server; [okhttp3.Response.request] still reports the original iCloud URL.
     */
    val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            val original: Request = chain.request()
            val rerouted: Request = original.newBuilder()
                .url(original.url.newBuilder().scheme("http").host(server.hostName).port(server.port).build())
                .header("Host", original.url.host)
                .build()
            chain.proceed(rerouted).newBuilder().request(original).build()
        })
        .build()

    override fun close() {
        server.shutdown()
    }
}

private class ICloudDispatcher : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        val contentType: String? = request.getHeader("Content-Type")
        if (request.method != "POST" || contentType?.startsWith("application/json") != true) {
            return MockResponse().setResponseCode(400).setBody("expected a JSON POST, got ${request.method} $contentType")
        }
        val host: String = request.getHeader("Host")!!
        val body: String = request.body.readUtf8()
        return when {
            request.path == "/B0R5qXGF1GeKg4d/sharedstreams/webstream" -> when {
                body != """{"streamCtag":null}""" -> MockResponse().setResponseCode(400).setBody("unexpected body $body")
                host == "p27-sharedstreams.icloud.com" -> json(200, loadResource("example_shares.json"))
                host.endsWith("-sharedstreams.icloud.com") -> json(330, """{"X-Apple-MMe-Host": "p27-sharedstreams.icloud.com"}""")
                else -> notFound()
            }
            request.path == "/B0R5qXGF1GeKg4d/sharedstreams/webasseturls" && host == "p27-sharedstreams.icloud.com" -> {
                val photoGuid: String = ((JsonParser.parseString(body) as JsonObject)["photoGuids"] as JsonArray)[0].asString
                json(200, loadResource("example_photo_$photoGuid.json"))
            }
            else -> notFound()
        }
    }

    private fun json(code: Int, body: String): MockResponse =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private fun notFound(): MockResponse = MockResponse().setResponseCode(404).setBody("Not found")
}

fun loadResource(res: String): String {
    val i: InputStream = checkNotNull(Thread.currentThread().contextClassLoader.getResourceAsStream(res)) {
        "Resource '$res' not found"
    }
    return i.use { it.reader().readText() }
}
