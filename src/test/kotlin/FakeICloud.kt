package com.github.mvysny.icloudphotopublicshare

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
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
 *     ICloudClient("B0R5qXGF1GeKg4d", httpClient = fake.client).getShare()
 * }
 * ```
 *
 * Knows album `B0R5qXGF1GeKg4d` on `p27-sharedstreams.icloud.com`; every other `pNN` host answers 330, redirecting there,
 * except `p99`, which redirects to itself forever, and `p98`, which redirects off iCloud. Album `Malformed` on `p27`
 * answers a photo without a `photoGuid`.
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
        val path: String = request.path!!
        val body: String = request.body.readUtf8()
        if (path.endsWith("/webstream") && body != """{"streamCtag":null}""") {
            return MockResponse().setResponseCode(400).setBody("unexpected body $body")
        }
        return when {
            path == "/Malformed/sharedstreams/webstream" && host == "p27-sharedstreams.icloud.com" ->
                json(200, """{"photos": [{"photoGuid": null, "derivatives": {}}]}""")
            !path.startsWith("/B0R5qXGF1GeKg4d/") -> notFound()
            host == "p99-sharedstreams.icloud.com" -> redirectTo(host)
            host == "p98-sharedstreams.icloud.com" -> redirectTo("example.com")
            host != "p27-sharedstreams.icloud.com" && host.endsWith("-sharedstreams.icloud.com") -> redirectTo("p27-sharedstreams.icloud.com")
            host != "p27-sharedstreams.icloud.com" -> notFound()
            path == "/B0R5qXGF1GeKg4d/sharedstreams/webstream" -> json(200, loadResource("example_shares.json"))
            path == "/B0R5qXGF1GeKg4d/sharedstreams/webasseturls" -> {
                val photoGuid: String = try {
                    ((JsonParser.parseString(body) as JsonObject)["photoGuids"] as JsonArray)[0].asString
                } catch (e: JsonParseException) {
                    return MockResponse().setResponseCode(400).setBody("malformed JSON $body")
                }
                val photo: String? = javaClass.classLoader.getResource("example_photo_$photoGuid.json")?.readText()
                if (photo == null) notFound() else json(200, photo)
            }
            else -> notFound()
        }
    }

    private fun redirectTo(host: String): MockResponse = json(330, """{"X-Apple-MMe-Host": "$host"}""")

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
