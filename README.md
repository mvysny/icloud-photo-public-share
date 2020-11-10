# iCloud Photo Public Share

[![pipeline status](https://gitlab.com/mvysny/icloud-photo-public-share/badges/master/pipeline.svg)](https://gitlab.com/mvysny/icloud-photo-public-share/commits/master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.gitlab.mvysny.icloud-photo-public-share/icloud-photo-public-share/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.gitlab.mvysny.icloud-photo-public-share/icloud-photo-public-share)

A Kotlin library which allows you to download photos from a publicly shared
iCloud Photos (public shared folder).

When you share a folder of photos in iCloud
you can also create a public website (with an obscure URL generated when the shared folder is made public),
such as `https://www.icloud.com/sharedalbum/#B0R5qXGF1GeKg4d`.
You can then pass this link to this library, to list the images and/or download them.

## How to use

```kotlin
fun main() {
    ICloud.httpClient = SimplifiedOkHttpClient(OkHttpClient())
    val client: ICloudClient = ICloudClient.parseURL("https://www.icloud.com/sharedalbum/#B0R5qXGF1GeKg4d")
    val share: ICloudShare = client.getShare()

    // download first photo
    val photo: ICloudPhoto = share.photos[0]
    val derivative: ICloudDerivative = photo.derivatives.values.maxByOrNull { it.height }!!

    // the URL is only valid for an hour or so
    val imageUrl: URL = client.getDownloadUrl(photo, derivative)
    println(imageUrl)
}
```
