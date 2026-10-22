# iCloud Photo Public Share

[![Build](https://github.com/mvysny/icloud-photo-public-share/actions/workflows/build.yml/badge.svg)](https://github.com/mvysny/icloud-photo-public-share/actions/workflows/build.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.mvysny.icloud-photo-public-share/icloud-photo-public-share/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.mvysny.icloud-photo-public-share/icloud-photo-public-share)

A Kotlin library which allows you to download photos from a publicly shared
iCloud Photos (public shared folder).

When you share a folder of photos in iCloud
you can also create a public website (with an obscure URL generated when the shared folder is made public),
such as `https://www.icloud.com/sharedalbum/#B0AbCdEfGhIjKlM`.
You can then pass this link to this library, to list the images and/or download them.

## Include in your project

The library is on Maven Central. Add the following to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}
dependencies {
    implementation("com.github.mvysny.icloud-photo-public-share:icloud-photo-public-share:0.4")
}
```

Or to your `pom.xml`:

```xml
<dependency>
    <groupId>com.github.mvysny.icloud-photo-public-share</groupId>
    <artifactId>icloud-photo-public-share</artifactId>
    <version>0.4</version>
</dependency>
```

Upgrading from 0.3? The groupId and the package changed, see [Compatibility](#compatibility).

## How to use

```kotlin
import com.github.mvysny.icloudphotopublicshare.*

fun main() {
    val client: ICloudClient = ICloudClient.parseURL("https://www.icloud.com/sharedalbum/#B0AbCdEfGhIjKlM")
    val share: ICloudShare = client.getShare()

    // download first photo
    val photo: ICloudPhoto = share.photos[0]
    val derivative: ICloudDerivative = photo.largestDerivative
    // or a thumbnail: photo.derivativeAtLeast(400) ?: photo.largestDerivative

    // the URL is only valid for an hour or so
    val imageUrl: String = client.getDownloadUrl(photo, derivative)
    println(imageUrl)
}
```

The URL is a `https://`- based URL from which the image can be downloaded directly -
no authentication needed.

Every `get*` call blocks on an HTTP request, so on Android keep it off the main thread. It throws
`IOException` on a network failure or an unexpected response, `FileNotFoundException` when the album
is gone.

`getDownloadUrl` costs one request per photo. To list many photos, fetch all their URLs in one request:

```kotlin
val webAsset: WebAsset = client.getWebAssets(share.photos)
val imageUrls: List<String> = share.photos.map { webAsset.getDownloadUrl(it.largestDerivative) }
```

All requests go through one shared `OkHttpClient`. Pass your own to configure timeouts, a proxy or a cache:

```kotlin
val httpClient = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()
val client: ICloudClient = ICloudClient.parseURL("https://www.icloud.com/sharedalbum/#B0AbCdEfGhIjKlM", httpClient)
```

With 0.4 also add `com.squareup.okhttp3:okhttp:4.12.0` to your dependencies: 0.4 publishes OkHttp in the runtime
scope only, so `OkHttpClient` won't compile without it. 0.5 fixes that.

## Compatibility

* 0.2: The library is compatible with JDK 7+ and Android API 8+ (that's Android 2.2.x).
* 0.3+: Compatible with Android 5.0+ (SDK21+) and JDK8+.
* 0.4+: `ICloudClient` and `parseURL` take an optional `OkHttpClient`; pass one to configure timeouts, a proxy or a
  cache. The `ICloud` global object, `SimplifiedHttpClient` and the public OkHttp helper functions are gone. The model
  (`ICloudShare`, `ICloudPhoto`, `WebAsset`, …) is read-only with non-null ids, and a response missing an id fails
  with `IOException`. `getDownloadUrl` returns a `String` rather than a `java.net.URL`, whose `equals` resolves the
  host by DNS. `ICloudPhoto.largestDerivative` and `derivativeAtLeast(px)` pick a size;
  `getWebAssets(photos)` fetches the URLs of many photos in one request. The jar ships its R8/ProGuard rules, so an
  app that shrinks its code needs none of its own. The groupId moves to
  `com.github.mvysny.icloud-photo-public-share` and the package to `com.github.mvysny.icloudphotopublicshare`; 0.3 and
  older stay under `com.gitlab.mvysny…`.

Depends on the Kotlin stdlib, OkHttp 4 and Gson; nothing else.

# License

See [LICENSE](LICENSE)

# Contributing / Developing / Releasing

See [Contributing](CONTRIBUTING.md) and [Releasing](RELEASING.md).
