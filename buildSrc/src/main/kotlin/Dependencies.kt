import Versions.kotlinVersion
import Versions.rxJavaVersion


object Versions {
    const val kotlinVersion = "1.3.70"
    const val rxJavaVersion = "3.0.9"
}

object BuildScriptDependencies {
    const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
    const val gradleAndroid = "com.android.tools.build:gradle:3.6.1"
    const val novodaBintray = "com.novoda:bintray-release:0.9.1"
}

object Dependencies {
    const val rxJava = "io.reactivex.rxjava3:rxjava:$rxJavaVersion"
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion"

    const val jUnit = "junit:junit:4.13"
    const val mockitoKotlin = "com.nhaarman:mockito-kotlin:1.5.0"
}


