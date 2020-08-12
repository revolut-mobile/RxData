import com.novoda.gradle.release.PublishExtension
import com.novoda.gradle.release.ReleasePlugin

buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath(BuildScriptDependencies.novodaBintray)
    }
}

plugins.apply(ReleasePlugin::class.java)

publishKotlinFix()

val publishProjectVersion: String by project
val publishGroupProjectId: String by project
val publishArtifactProjectId: String by project
val publishUserOrg: String by project
val publishRepoName: String by project
val publishDescription: String by project
val publishWebsite: String by project

configure<PublishExtension> {
    bintrayUser = BINTRAY_USER
    bintrayKey = BINTRAY_KEY

    userOrg = publishUserOrg
    repoName = publishRepoName
    groupId = publishGroupProjectId
    artifactId = publishArtifactProjectId
    publishVersion = publishProjectVersion
    desc = publishDescription
    website = publishWebsite
}