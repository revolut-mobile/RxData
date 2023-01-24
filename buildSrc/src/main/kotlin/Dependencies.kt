import Versions.coroutinesCoreVersion
import Versions.junitVersion
import Versions.kotlinVersion
import Versions.rxJavaVersion


object Versions {
    const val kotlinVersion = "1.6.10"
    const val rxJavaVersion = "2.2.18"
    const val coroutinesCoreVersion = "1.6.0"
    const val junitVersion = "5.7.2"
}

object BuildScriptDependencies {
    const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
    const val gradleAndroid = "com.android.tools.build:gradle:7.0.4"
}

object Dependencies {
    const val rxJava = "io.reactivex.rxjava2:rxjava:$rxJavaVersion"
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion"

    const val coroutinesCore =
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesCoreVersion"
    const val coroutinesCoreTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesCoreVersion"
    const val turbineCoroutinesTest = "app.cash.turbine:turbine:0.7.0"

    const val jupiterApi = "org.junit.jupiter:junit-jupiter-api:$junitVersion"
    const val jupiterParams = "org.junit.jupiter:junit-jupiter-params:$junitVersion"
    const val jupiterEngine = "org.junit.jupiter:junit-jupiter-engine:$junitVersion"
    const val mockitoKotlin = "org.mockito.kotlin:mockito-kotlin:4.0.0"
    const val mockitoInline = "org.mockito:mockito-inline:3.8.0"
}


