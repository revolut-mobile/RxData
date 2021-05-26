plugins {
    kotlin("jvm")
}

dependencies {
    implementation(Dependencies.coroutinesCore)
    implementation(Dependencies.kotlinStdLib)

    testImplementation(Dependencies.jUnit)
    testImplementation(Dependencies.mockitoKotlin)
}
