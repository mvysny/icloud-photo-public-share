package com.gitlab.mvysny.icloudphotopublicshare

import java.io.FileNotFoundException
import java.io.IOException

/**
 * A very simple http client which the [ICloudClient] uses to communicate with iCloud.
 */
public interface SimplifiedHttpClient {
    /**
     * Sends a HTTP POST to given [url]. Posts [postJson] as the POST request body.
     * The POST request must send the `Content-Type: application/json` HTTP header.
     * @param responseHandler handles the response.
     * @return whatever [responseHandler] returned.
     */
    public fun <T> post(url: String, postJson: String, responseHandler: (HttpResponse) -> T): T
}

public interface HttpResponse {
    /**
     * The original URL of the request, see [SimplifiedHttpClient.post] `url` parameter.
     */
    public val url: String

    /**
     * The resulting HTTP code.
     */
    public val code: Int

    /**
     * True if [code] is 2xy.
     */
    public val isSuccessful: Boolean get() = code in 200..299

    /**
     * Fails with [IOException] if [isSuccessful] is false.
     */
    public fun checkOk() {
        if (!isSuccessful) {
            val msg = "${code}: ${string()} ($url)"
            if (code == 404) throw FileNotFoundException(msg)
            throw IOException(msg)
        }
    }

    /**
     * Reads the entire response as a string and returns it.
     */
    public fun string(): String

    /**
     * Parses the response as a JSON file and uses Gson to map the JSON into
     * a Java POJO.
     * @param T the type of the Java POJO.
     * @param clazz the class of the Java POJO.
     * @return populated instance of the POJO.
     */
    public fun <T> json(clazz: Class<T>): T
}
