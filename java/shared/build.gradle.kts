/*
 * This file was generated by the Gradle "init" task.
 */

import subzero.DependencyVersions

plugins {
    id("com.squareup.subzero.java-conventions")
}

dependencies {
    api(project(":proto"))
    api("com.google.code.findbugs:jsr305:${DependencyVersions.findbugs}")
    api("com.google.guava:guava:${DependencyVersions.guava}")
    api("com.google.zxing:core:${DependencyVersions.zxing}")
    api("org.bouncycastle:bcprov-jdk18on:${DependencyVersions.bouncycastle}")
    api("org.bitcoinj:bitcoinj-core:${DependencyVersions.bitcoinj}") {
        exclude("com.google.protobuf", "protobuf-javalite")
    }
    testImplementation("junit:junit:${DependencyVersions.junit}")
}

description = "shared"

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            groupId = group.toString()
            artifactId = "shared"
            version = version
            from(components["java"])
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}
