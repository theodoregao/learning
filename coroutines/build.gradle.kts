plugins {
    java
    id("org.jetbrains.kotlin.jvm") version "1.5.20"
}

group = "shun.gao"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testCompile("junit", "junit", "4.12")
}
