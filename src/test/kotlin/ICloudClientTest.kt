package com.github.mvysny.icloudphotopublicshare

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.io.IOException
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
            val url: String = client.getDownloadUrl("1F6DA863-398D-4BA6-9980-350C982F6276", "015c281ffb89b0c02204d548d5720b9132dd4bfb93")
            assertContains(url, "/IMG_0066.JPG?")
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
        fun `photo guid is escaped in the request`() {
            // unescaped, the fake would answer 400 to the malformed JSON
            expect(mapOf()) { client().getWebAsset("a\"b").items }
        }

        @Test
        fun `web assets of the whole album in one request`() {
            val client = client()
            val photos: List<ICloudPhoto> = client.getShare().photos
            val webAsset: WebAsset = client.getWebAssets(photos)
            expect(2) { fakeICloud.requestCount }
            for (photo in photos) {
                for (derivative in photo.derivatives.values) {
                    assertContains(webAsset.getDownloadUrl(derivative), "https://cvws.icloud-content.com/S/")
                }
            }
        }

        @Test
        fun `web assets of no photos cost no request`() {
            expect(mapOf()) { client().getWebAssets(listOf<String>()).items }
            expect(0) { fakeICloud.requestCount }
        }

        @Test
        fun `unknown photo guid has no derivatives`() {
            val photo: ICloudPhoto = client().getShare().photos.first()
            val webAsset: WebAsset = client().getWebAssets(listOf(photo.photoGuid, "00000000-0000-0000-0000-000000000000"))
            expect(photo.derivatives.values.map { it.checksum }.toSet()) { webAsset.items.keys }
            val ex = assertFailsWith<IllegalStateException> {
                client().getDownloadUrl("00000000-0000-0000-0000-000000000000", photo.largestDerivative.checksum)
            }
            assertContains(ex.message!!, "No item with checksum '${photo.largestDerivative.checksum}'")
        }

        @Test
        fun `missing id fails`() {
            val ex = assertFailsWith<IOException> {
                client("Malformed").getShare()
            }
            assertContains(ex.message!!, "Malformed iCloud response, missing photoGuid (https://p27-sharedstreams.icloud.com/Malformed/sharedstreams/webstream)")
        }

        @Test
        fun `derivative by size`() {
            val photo: ICloudPhoto = client().getShare().photos.first()
            expect("015c281ffb89b0c02204d548d5720b9132dd4bfb93") { photo.largestDerivative.checksum }
            expect("01a9c356dc72e68ff5a74dbe67ee01d7fc10f3762f") { photo.derivativeAtLeast(300)?.checksum }
            expect("01a9c356dc72e68ff5a74dbe67ee01d7fc10f3762f") { photo.derivativeAtLeast(342)?.checksum }
            expect("015c281ffb89b0c02204d548d5720b9132dd4bfb93") { photo.derivativeAtLeast(343)?.checksum }
            expect(null) { photo.derivativeAtLeast(1025) }
        }

        @Test
        fun `photo url`() {
            val client = client()
            val photo: ICloudPhoto = client.getShare().photos.first()
            val url: String = client.getDownloadUrl(photo.photoGuid, "015c281ffb89b0c02204d548d5720b9132dd4bfb93")
            expect("https://cvws.icloud-content.com/S/AVwoH_uJsMAiBNVI1XILkTLdS_uT/IMG_0066.JPG?o=AgRYjMCqvh4dqcQDfc740fbIMnnbGNGZrHl-gJXPo9oq&v=1&z=https%3A%2F%2Fp137-content.icloud.com%3A443&x=1&a=CAogwyOAxLMQwsIY4MsH8OjMwbnsmSSobjzs0Jl85SYQN3USZxCi5pCTljQYooXak5Y0IgEAUgTdS_uTaibzrmhK3f2Fvq36KI0upIpBKKvpNnQBK3bGse3_PYyeHLaqA88ikHImfU-gxk5kqlvz6A96T1GXMUdN-R7iRQBuShfeIY66mUIshhtFmVg&e=1792653296&r=20f55cf4-8728-402c-a496-c8cb902d4bfe-55&s=Km39Oh9MqQNwgK0XSIVkihw3k7A") {
                url
            }
        }
    }
}
