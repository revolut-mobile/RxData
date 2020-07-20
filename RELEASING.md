# Releasing

Each project is released separately.<br>
The release tag name (version) prefix is **mandatory** and used to determine which project to release and upload to the Bintray.


 1. Change the version in `gradle.properties` (from subproject directory) to a non-SNAPSHOT version.
 2. Update the `CHANGELOG.md` for the impending release.
 3. Update the `README.md` with the new version.
 4. Go to [releases](https://github.com/revolut-mobile/RxData/releases) and create new release. The `tag version` must have the prefix.