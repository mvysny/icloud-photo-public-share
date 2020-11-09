package com.gitlab.mvysny.icloudphotopublicshare

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectThrows
import java.io.FileNotFoundException
import java.lang.IllegalArgumentException
import java.net.URL
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
        test("photo url") {
            val client = ICloudClient("B0R5qXGF1GeKg4d")
            val photo: ICloudPhoto = client.getShare().photos.first()
            val url: URL = client.getDownloadUrl(photo.photoGuid!!, "015c281ffb89b0c02204d548d5720b9132dd4bfb93")
            expect("https://cvws.icloud-content.com/S/AVwoH_uJsMAiBNVI1XILkTLdS_uT/IMG_0050.JPG?o=Ah9jlrymSJpGvxzFyw5LRyXJJNoTsLu0472KzMOew_Ax&v=1&z=https%3A%2F%2Fp27-content.icloud.com%3A443&x=1&a=CAogakRqhS9LcLGye2zEnNf62yVFCT-b5kPSkfRfXo9p6iQSZxDsuprk2i4Y7NGt6douIgEAUgTdS_uTaia6pw47lOJbhJdNDzPgkgbHS67GO_nU-MIHjJUt6x8dpn_OnJyXWXImtrCSgVzEAPkKXOd2RFIbrGnbA3adQE1mQuNLvkhot9syoVl2nSY&e=1604928104&r=18253835-00d8-48f1-8bf2-255bb9167f19-1&s=DVy1ZCRXorj-P_BPiPH8HHfNrlE") {
                url.toString()
            }
        }
    }
})
