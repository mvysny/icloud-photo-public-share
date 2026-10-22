# Releasing

To release the library to Maven Central:

1. Edit `build.gradle.kts` and remove `-SNAPSHOT` in the `version=` stanza, e.g. "0.4"
2. Edit `README.md`: bump the version in the snippet under "Include in your project" to match the release,
   and add a line to "Compatibility" if the supported JDK or Android version changed
3. Switch to JDK 11 or lower, to ensure Android compatibility; the build refuses to publish otherwise.
   Note that `mise.toml` pins JDK 17.
4. Run `./gradlew clean build publish closeAndReleaseStagingRepositories`
5. (Optional) watch [Maven Central Publishing Deployments](https://central.sonatype.com/publishing/deployments) as the deployment is published.
6. Commit with the commit message of simply being the version being released, e.g. "0.4"
7. git tag the commit with the same tag name as the commit message above, e.g. `0.4`
8. `git push`, `git push --tags`
9. Add the `-SNAPSHOT` back to the `version=` while increasing the version to something which will be released in the future,
   e.g. 0.5-SNAPSHOT, then commit with the commit message "0.5-SNAPSHOT" and push.
