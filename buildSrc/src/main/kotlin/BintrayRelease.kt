import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.get

val BINTRAY_USER get() = System.getenv("BINTRAY_USER")
val BINTRAY_KEY get() = System.getenv("BINTRAY_KEY")

fun Project.publishKotlinFix() {
    tasks.whenTaskAdded {
        if (name == "generateSourcesJarForMavenPublication") {
            this as Jar
            from(
                project.convention.getPlugin(JavaPluginConvention::class.java)
                    .sourceSets["main"].allSource
            )
        }
    }
}