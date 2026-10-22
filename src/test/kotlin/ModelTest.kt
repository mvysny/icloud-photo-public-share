package com.github.mvysny.icloudphotopublicshare

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.expect

class ModelTest {
    @Nested
    inner class ICloudPhotoTests {
        private val photo = ICloudPhoto(null, mapOf(), null, null, "1F6DA863", null, null, 0, 0, null)

        @Test
        fun `photo without derivatives`() {
            val ex = assertFailsWith<IllegalStateException> { photo.largestDerivative }
            assertContains(ex.message!!, "Photo 1F6DA863 has no derivatives")
            expect(null) { photo.derivativeAtLeast(0) }
        }
    }

    @Nested
    inner class WebAssetTests {
        private val item = WebItem(null, "cvws.icloud-content.com", "/S/abc/IMG_0050.JPG?o=1")

        @Test
        fun `location provides scheme and host`() {
            val webAsset = WebAsset(mapOf("cvws.icloud-content.com" to ILocation("http", listOf("a.example.com", "b.example.com"))), mapOf("c1" to item))
            expect("http://a.example.com/S/abc/IMG_0050.JPG?o=1") { webAsset.getDownloadUrl("c1") }
        }

        @Test
        fun `https and url_location as host when the location lacks them`() {
            val webAsset = WebAsset(mapOf("cvws.icloud-content.com" to ILocation(null, listOf())), mapOf("c1" to item))
            expect("https://cvws.icloud-content.com/S/abc/IMG_0050.JPG?o=1") { webAsset.getDownloadUrl("c1") }
        }

        @Test
        fun `no location at all`() {
            expect("https://cvws.icloud-content.com/S/abc/IMG_0050.JPG?o=1") { WebAsset(mapOf(), mapOf("c1" to item)).getDownloadUrl("c1") }
        }

        @Test
        fun `unknown checksum`() {
            val ex = assertFailsWith<IllegalStateException> { WebAsset(mapOf(), mapOf("c1" to item)).getDownloadUrl("c2") }
            assertContains(ex.message!!, "No item with checksum 'c2'")
        }
    }

    @Nested
    inner class JsonMappingTests {
        @Test
        fun `missing lists map to empty ones`() {
            expect(listOf()) { ShareJson().toShare("url").photos }
            expect(mapOf()) { ShareJson(photos = listOf(PhotoJson(photoGuid = "1F6DA863"))).toShare("url").photos[0].derivatives }
            expect(WebAsset(mapOf(), mapOf())) { WebAssetJson().toWebAsset("url") }
            expect(listOf()) { WebAssetJson(locations = mapOf("cvws" to LocationJson())).toWebAsset("url").locations.getValue("cvws").hosts }
        }
    }
}
