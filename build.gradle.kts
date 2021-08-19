import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
    `maven-publish`
    signing
}

defaultTasks("clean", "build")

group = "com.gitlab.mvysny.icloud-photo-public-share"
version = "0.2-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.6"  // Android compatibility
    if (System.getProperty("kotlin.jdkHome") != null) {
        kotlinOptions.jdkHome = System.getProperty("kotlin.jdkHome")
    }
}

kotlin {
    explicitApi()
}

dependencies {
    api(kotlin("stdlib")) // don't use -jdk8 to stay compatible with Android
    // Keep okhttp at 3.12.x to stay compatible with older Androids: https://medium.com/square-corner-blog/okhttp-3-13-requires-android-5-818bb78d07ce
    // okhttp 3.12.x supports Android API 9+: https://github.com/square/okhttp
    // okhttp 3.13.x supports only Android API 21+
    implementation("com.squareup.okhttp3:okhttp:3.12.2")
    implementation("com.google.code.gson:gson:2.8.7")

    testImplementation("com.github.mvysny.dynatest:dynatest-engine:0.20")
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<Javadoc> {
    isFailOnError = false
}

publishing {
    repositories {
        maven {
            setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.properties["ossrhUsername"] as String? ?: "Unknown user"
                password = project.properties["ossrhPassword"] as String? ?: "Unknown user"
            }
        }
    }
    publications {
        create("mavenJava", MavenPublication::class.java).apply {
            groupId = project.group.toString()
            this.artifactId = "icloud-photo-public-share"
            version = project.version.toString()
            pom {
                description.set("iCloud photo public share client in Kotlin")
                name.set("VoK-ORM")
                url.set("https://gitlab.com/mvysny/icloud-photo-public-share")
                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("mavi")
                        name.set("Martin Vysny")
                        email.set("martin@vysny.me")
                    }
                }
                scm {
                    url.set("https://gitlab.com/mvysny/icloud-photo-public-share")
                }
            }
            from(components["java"])
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        // to see the exceptions of failed tests in the CI console.
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

