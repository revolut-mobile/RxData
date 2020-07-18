# Releasing

Each project is released separately.<br>
The tag name prefix is **mandatory** and used to determine which project to release and upload to the Bintray.<br>
Replace `${project}` with the project artifact ID and `X.Y.Z` with the new version.

 1. Change the version in `gradle.properties` (from subproject directory) to a non-SNAPSHOT version.
 2. Update the `CHANGELOG.md` for the impending release.
 3. Update the `README.md` with the new version.
 4. `git commit -am "${project}: prepare for release X.Y.Z"` to commit changes with the new version.
 5. `git tag -a ${project}-vX.Y.Z -m "${project}: version X.Y.Z"` to create the new tag.<br>
  Example for `core` and version `1.2.3`:<br>
  `git tag -a core-v1.2.3 -m "core: version 1.2.3"`
 
 6. Update the `gradle.properties` (from the subproject directory) to the next SNAPSHOT version.
 7. `git push && git push --tags` to push the changes with the new version and tag which will trigger the CI release task.