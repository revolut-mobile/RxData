plugins {
    `kotlin-dsl`
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        //Kotlin extensions can only be accessed via fully qualified names inside buildscript block
        //https://github.com/gradle/gradle/issues/9270
        classpath(BuildScriptDependencies.gradleAndroid)
        classpath(BuildScriptDependencies.gradlePlugin)

        classpath("com.vanniktech:gradle-maven-publish-plugin:0.13.0")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.4.10.2")
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
        jcenter {
            content {
                // https://youtrack.jetbrains.com/issue/IDEA-261387
                includeModule("org.jetbrains.trove4j", "trove4j")
                includeModule("org.jetbrains.dokka", "dokka-gradle-plugin")
                includeModule("org.jetbrains.kotlinx", "kotlinx-html-jvm")
            }
        }
    }
}
