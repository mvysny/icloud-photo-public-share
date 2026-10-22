import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.4.21"
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("org.jetbrains.kotlinx.kover") version "0.9.11"
}

defaultTasks("clean", "build")

group = "com.github.mvysny.icloud-photo-public-share"
version = "0.4"

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget = JvmTarget.JVM_1_8  // Android 5.0 compatibility
}

kotlin {
    explicitApi()
}

// JDK 12+ adds Java members that collide with Kotlin stdlib ones (seen with the Stream API); kotlinc then prefers the
// Java member, which Android lacks, so the call throws there at runtime.
if (JavaVersion.current() > JavaVersion.VERSION_11 && gradle.startParameter.taskNames.contains("publish")) {
    throw GradleException("Release this library with JDK 11 or lower, to ensure Android compatibility; current JDK is ${JavaVersion.current()}")
}

dependencies {
    api(kotlin("stdlib")) // don't use -jdk8 to stay compatible with Android
    // stays on 4.x for now: OkHttp 5 splits into separate JVM and Android artifacts
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.14.0")

    testImplementation(kotlin("test"))
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    // JUnit 6 needs JDK 17, but CI still tests on JDK 8
    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<Javadoc> {
    isFailOnError = false
}

publishing {
    publications {
        create("mavenJava", MavenPublication::class.java).apply {
            groupId = project.group.toString()
            this.artifactId = "icloud-photo-public-share"
            version = project.version.toString()
            pom {
                description.set("iCloud photo public share client in Kotlin")
                name.set("iCloud Client")
                url.set("https://github.com/mvysny/icloud-photo-public-share")
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
                    url.set("https://github.com/mvysny/icloud-photo-public-share")
                }
            }
            from(components["java"])
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

nexusPublishing {
    repositories {
        // see https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#configuration
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}

// the doc layer's checks, see AGENTS.md; CI runs them in a job of their own
val verifyDesignTripwires by tasks.registering(Exec::class) {
    commandLine("design/verify_design_tripwires.sh")
    onlyIf { System.getenv("CI") == null }
}
tasks.check { dependsOn(verifyDesignTripwires) }

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        // to see the exceptions of failed tests in the CI console.
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

