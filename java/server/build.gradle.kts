/*
 * This file was generated by the Gradle "init" task.
 */

import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.squareup.subzero.java-conventions")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation("io.dropwizard:dropwizard-core:2.1.1")
    implementation(project(":proto"))
    implementation("com.google.protobuf:protobuf-java-util:3.23.4")
    implementation(project(":shared"))
    implementation("io.dropwizard:dropwizard-assets:2.1.1")
}

description = "server"

tasks {
    // Disable non-shadow jar generation
    named<Jar>("jar") {
        enabled = false
    }

    // Configure shadow jar generation
    named<ShadowJar>("shadowJar") {
        enabled = true
        mergeServiceFiles()
        archiveClassifier.set("")
        manifest {
            attributes["Main-Class"] = "com.squareup.subzero.server.ServerApplication"
        }
        // signatures from foreign jars are bad news
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }

    // Make sure "gradle build" generates the shadow jar
    named("assemble") {
        dependsOn(":server:shadowJar")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            project.extensions.configure<ShadowExtension>() {
                component(this@create)
            }
        }
    }
}
