package com.gitlab.mvysny.icloudphotopublicshare

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectThrows
import java.lang.IllegalArgumentException
import kotlin.test.expect

class ICloudClientTest : DynaTest({
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
})
