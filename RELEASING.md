# Releasing

Each project is released separately.<br>
The release tag name (version) prefix is **mandatory** and used to determine which project to release and upload to the Bintray.


 1. Change the version in `gradle.properties` (from subproject directory) to a non-SNAPSHOT version.
 2. Update the `README.md` with the new version.
 3. Go to [releases](https://github.com/revolut-mobile/RxData/releases) and create new release. The `tag version` must have the prefix, e.g. dod-1.4-migrate-to-maven
 4. This will trigger uploading to maven staging repository, in order to propagate this release you need to go to https://s01.oss.sonatype.org/ and close the staging
 5. When staging is closed, Release the library via the Nexus interface
 
 