package com.gitlab.mvysny.icloudphotopublicshare

import java.io.InputStream
import kotlin.test.expect

class DummyHttpClient : SimplifiedHttpClient {
    override fun <T> post(url: String, postJson: String, responseHandler: (HttpResponse) -> T): T {
        val response: HttpResponse = when {
            url == "https://p27-sharedstreams.icloud.com/B0R5qXGF1GeKg4d/sharedstreams/webstream" -> {
                expect("""{"streamCtag":null}""") { postJson }
                DummyHttpResponse(url, 200, loadResource("example_shares.json"))
            }
            url.endsWith("-sharedstreams.icloud.com/B0R5qXGF1GeKg4d/sharedstreams/webstream") -> {
                expect("""{"streamCtag":null}""") { postJson }
                DummyHttpResponse(url, 330, """{"X-Apple-MMe-Host": "p27-sharedstreams.icloud.com"}""")
            }
            else -> DummyHttpResponse(url, 404, "Not found")
        }
        return responseHandler(response)
    }

}

fun loadResource(res: String): String {
    val i: InputStream = checkNotNull(Thread.currentThread().contextClassLoader.getResourceAsStream(res)) {
        "Resource '$res' not found"
    }
    return i.use { it.reader().readText() }
}

class DummyHttpResponse(override val url: String, override val code: Int, val result: String) : HttpResponse {
    override fun string(): String = result

    override fun <T> json(clazz: Class<T>): T =
            ICloud.gson.fromJson(string(), clazz)
}
