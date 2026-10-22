package com.gitlab.mvysny.icloudphotopublicshare

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.expect

class ICloudClientTest {
    @Nested
    inner class URLParsing {
        @Test
        fun simple() {
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

        @Test
        fun `icloud-com URLs`() {
            expect("ICloudClient(albumId='B0R5qXGF1GeKg4d', serverName='p27-sharedstreams.icloud.com')") {
                ICloudClient.parseURL("https://www.icloud.com/sharedalbum/#B0R5qXGF1GeKg4d").toString()
            }
        }

        @Test
        fun `incorrect URL`() {
            for (url in listOf(
                "https://www.google.com",
                "https://www.icloud.com/sharedalbum/#",
                "https://www.icloud.com/sharedalbum/#abc/def",
                "https://p27-sharedstreamsXicloud.com/B0R5qXGF1GeKg4d/sharedstreams",
            )) {
                val ex = assertFailsWith<IllegalArgumentException>(url) {
                    ICloudClient.parseURL(url)
                }
                assertContains(ex.message!!, "Failed to parse $url: not an iCloud shared album URL")
            }
        }

        @Test
        fun `invalid album id`() {
            val ex = assertFailsWith<IllegalArgumentException> {
                ICloudClient("abc/def")
            }
            assertContains(ex.message!!, "Invalid album ID 'abc/def'")
        }

        @Test
        fun `invalid server name`() {
            val ex = assertFailsWith<IllegalArgumentException> {
                ICloudClient("B0R5qXGF1GeKg4d", "example.com")
            }
            assertContains(ex.message!!, "Invalid server name 'example.com'")
        }
    }

    @Nested
    inner class FakeICloudTests {
        private lateinit var fakeICloud: FakeICloud

        @BeforeEach
        fun startFakeICloud() {
            fakeICloud = FakeICloud()
        }

        @AfterEach
        fun stopFakeICloud() {
            fakeICloud.close()
        }

        private fun client(albumId: String = "B0R5qXGF1GeKg4d", serverName: String = "p27-sharedstreams.icloud.com") =
            ICloudClient(albumId, serverName, fakeICloud.client)

        @Test
        fun smoke() {
            val client = client()
            expect(86) { client.getShare().photos.size }
            expect("ICloudClient(albumId='B0R5qXGF1GeKg4d', serverName='p27-sharedstreams.icloud.com')") { client.toString() }
        }

        @Test
        fun redirect() {
            val client = client(serverName = "p23-sharedstreams.icloud.com")
            expect(86) { client.getShare().photos.size }
            expect("ICloudClient(albumId='B0R5qXGF1GeKg4d', serverName='p27-sharedstreams.icloud.com')") { client.toString() }
        }

        @Test
        fun `photo url follows redirect`() {
            val client = client(serverName = "p23-sharedstreams.icloud.com")
            val url: URL = client.getDownloadUrl("1F6DA863-398D-4BA6-9980-350C982F6276", "015c281ffb89b0c02204d548d5720b9132dd4bfb93")
            assertContains(url.toString(), "/IMG_0050.JPG?")
            expect("ICloudClient(albumId='B0R5qXGF1GeKg4d', serverName='p27-sharedstreams.icloud.com')") { client.toString() }
        }

        @Test
        fun `redirect loop fails`() {
            val ex = assertFailsWith<IOException> {
                client(serverName = "p99-sharedstreams.icloud.com").getShare()
            }
            assertContains(ex.message!!, "330: more than 5 redirects (https://p99-sharedstreams.icloud.com/B0R5qXGF1GeKg4d/sharedstreams/webstream)")
        }

        @Test
        fun `redirect to unexpected host fails`() {
            val ex = assertFailsWith<IOException> {
                client(serverName = "p98-sharedstreams.icloud.com").getShare()
            }
            assertContains(ex.message!!, "330: redirect to unexpected host example.com (https://p98-sharedstreams.icloud.com/")
        }

        @Test
        fun `404 reporting`() {
            val ex = assertFailsWith<FileNotFoundException> {
                client("nonexisting", "p23-sharedstreams.icloud.com").getShare()
            }
            assertContains(ex.message!!, "404: Not found (https://p23-sharedstreams.icloud.com/nonexisting/")
        }

        @Test
        fun `missing id fails`() {
            val ex = assertFailsWith<IOException> {
                client("Malformed").getShare()
            }
            assertContains(ex.message!!, "Malformed iCloud response, missing photoGuid (https://p27-sharedstreams.icloud.com/Malformed/sharedstreams/webstream)")
        }

        @Test
        fun `photo url`() {
            val client = client()
            val photo: ICloudPhoto = client.getShare().photos.first()
            val url: URL = client.getDownloadUrl(photo.photoGuid, "015c281ffb89b0c02204d548d5720b9132dd4bfb93")
            expect("https://cvws.icloud-content.com/S/AVwoH_uJsMAiBNVI1XILkTLdS_uT/IMG_0050.JPG?o=Ah9jlrymSJpGvxzFyw5LRyXJJNoTsLu0472KzMOew_Ax&v=1&z=https%3A%2F%2Fp27-content.icloud.com%3A443&x=1&a=CAogakRqhS9LcLGye2zEnNf62yVFCT-b5kPSkfRfXo9p6iQSZxDsuprk2i4Y7NGt6douIgEAUgTdS_uTaia6pw47lOJbhJdNDzPgkgbHS67GO_nU-MIHjJUt6x8dpn_OnJyXWXImtrCSgVzEAPkKXOd2RFIbrGnbA3adQE1mQuNLvkhot9syoVl2nSY&e=1604928104&r=18253835-00d8-48f1-8bf2-255bb9167f19-1&s=DVy1ZCRXorj-P_BPiPH8HHfNrlE") {
                url.toString()
            }
        }
    }
}
