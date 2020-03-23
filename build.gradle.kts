plugins {
    `kotlin-dsl`
}

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        //Kotlin extensions can only be accessed via fully qualified names inside buildscript block
        //https://github.com/gradle/gradle/issues/9270
        classpath(BuildScriptDependencies.gradleAndroid)
        classpath(BuildScriptDependencies.gradlePlugin)
        classpath(BuildScriptDependencies.novodaBintray)
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}
