import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

        classpath("com.vanniktech:gradle-maven-publish-plugin:0.18.0")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.4.10.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.20")
    }
}

allprojects {
//    plugins.withId("com.vanniktech.maven.publish") {
//        mavenPublish {
//            sonatypeHost = "S01"
//        }
//    }
    repositories {
        mavenCentral()
        google()
//        jcenter {
//            content {
//                // https://youtrack.jetbrains.com/issue/IDEA-261387
//                includeModule("org.jetbrains.trove4j", "trove4j")
//                includeModule("org.jetbrains.dokka", "dokka-gradle-plugin")
//                includeModule("org.jetbrains.kotlinx", "kotlinx-html-jvm")
//            }
//        }
    }
}

subprojects {
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
    tasks.withType<JavaCompile> {
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    }
}
