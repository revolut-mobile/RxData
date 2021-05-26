import Versions.coroutinesCoreVersion
import Versions.kotlinVersion
import Versions.rxJavaVersion


object Versions {
    const val kotlinVersion = "1.4.31"
    const val rxJavaVersion = "2.2.18"
    const val coroutinesCoreVersion = "1.5.0"
}

object BuildScriptDependencies {
    const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
    const val gradleAndroid = "com.android.tools.build:gradle:3.6.1"
}

object Dependencies {
    const val rxJava = "io.reactivex.rxjava2:rxjava:$rxJavaVersion"
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion"

    const val coroutinesCore =
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesCoreVersion"

    const val coroutinesCoreTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesCoreVersion"

    const val jUnit = "junit:junit:4.13"
    const val mockitoKotlin = "com.nhaarman:mockito-kotlin:1.5.0"
}


