package com.gitlab.mvysny.icloudphotopublicshare

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectThrows
import java.io.FileNotFoundException
import java.lang.IllegalArgumentException
import kotlin.test.expect

class ICloudClientTest : DynaTest({
    beforeGroup { ICloud.httpClient = DummyHttpClient() }

    group("URL parsing") {
        test("simple") {
            expect("ICloudClient(albumId='B0R5qXGF1GeKg4d', serverName='p23-sharedstreams.icloud.com')") {
                ICloudClient.parseURL("https://p23-sharedstreams.icloud.com/B0R5qXGF1GeKg4d/sharedstreams/webstream").toString()
            }
            expect("ICloudClient(albumId='B0R5qXGF1GeKg4d', serverName='p23-sharedstreams.icloud.com')") {
                ICloudClient.parseURL("https://p23-sharedstreams.icloud.com/B0R5qXGF1GeKg4d/sharedstreams").toString()
            }
            expect("ICloudClient(albumId='B0R5qXGF1GeKg4d', serverName='p27-sharedstreams.icloud.com')") {
                ICloudClient.parseURL("https://p27-sharedstreams.icloud.com/B0R5qXGF1GeKg4d/sharedstreams/webstream").toString()
            }
        }

        test("incorrect URL") {
            expectThrows(IllegalArgumentException::class, "Failed to parse https://www.google.com: not a sharedstreams.icloud.com URL") {
                ICloudClient.parseURL("https://www.google.com")
            }
        }
    }

    group("dummy http client") {
        test("smoke") {
            val client = ICloudClient("B0R5qXGF1GeKg4d")
            expect(86) { client.getShare().photos.size }
            expect("ICloudClient(albumId='B0R5qXGF1GeKg4d', serverName='p27-sharedstreams.icloud.com')") { client.toString() }
        }
        test("redirect") {
            val client = ICloudClient("B0R5qXGF1GeKg4d", "p23-sharedstreams.icloud.com")
            expect(86) { client.getShare().photos.size }
            expect("ICloudClient(albumId='B0R5qXGF1GeKg4d', serverName='p27-sharedstreams.icloud.com')") { client.toString() }
        }
        test("404 reporting") {
            val client = ICloudClient("nonexisting", "p23-sharedstreams.icloud.com")
            expectThrows(FileNotFoundException::class, "404: Not found (https://p23-sharedstreams.icloud.com/nonexisting/") {
                client.getShare()
            }
        }
    }
})
