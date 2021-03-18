val isReleasedVersion = !project.version.toString().endsWith("-SNAPSHOT")

val mavenCentralRepositoryUsername : String? by project
val mavenCentralRepositoryPassword : String? by project

configure<PublishingExtension> {
    repositories {
        maven {
            if (isReleasedVersion) {
                setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            } else {
                setUrl("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            }
            credentials {
                username = mavenCentralRepositoryUsername
                password = mavenCentralRepositoryPassword
            }
        }
    }
}