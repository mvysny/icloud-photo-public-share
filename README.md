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

The URL is a `https://`- based URL from which the image can be downloaded directly -
no authentication needed.

## Include in your project

Add the following to your `build.gradle`:

```groovy
repositories {
    mavenCentral()
}
dependencies {
    implementation('com.gitlab.mvysny.icloud-photo-public-share:icloud-photo-public-share:0.2')
}
```

## Compatibility

* 0.2: The library is compatible with JDK 7+ and Android API 8+ (that's Android 2.2.x).
* 0.3+: Compatible with Android 5.0+ (SDK21+) and JDK8+.
* 0.4+: `ICloudClient` and `parseURL` take an optional `OkHttpClient`; pass one to configure timeouts, a proxy or a
  cache. The `ICloud` global object, `SimplifiedHttpClient` and the public OkHttp helper functions are gone. The model
  (`ICloudShare`, `ICloudPhoto`, `WebAsset`, …) is read-only with non-null ids, and a response missing an id fails
  with `IOException`.

Requires Kotlin and OkHttp.

# License

See [LICENSE](LICENSE)

# Contributing / Developing / Releasing

See [Contributing](CONTRIBUTING.md) and [Releasing](RELEASING.md).
