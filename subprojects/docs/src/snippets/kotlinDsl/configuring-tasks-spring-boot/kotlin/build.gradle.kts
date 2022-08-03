// tag::lazy[]
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun

// tag::accessors[]
plugins {
    java
    id("org.springframework.boot") version "2.4.5"
    // TODO:Spring-Dependency-Management - update to version > 1.0.12.RELEASE (direct or transitive dependencies) to fix this snippet
}

// end::lazy[]
// end::accessors[]

// tag::accessors[]
tasks.bootJar {
    archiveFileName.set("app.jar")
    mainClassName = "com.example.demo.Demo"
}

tasks.bootRun {
    mainClass.set("com.example.demo.Demo")
    args("--spring.profiles.active=demo")
}
// end::accessors[]

// tag::lazy[]
tasks.named<BootJar>("bootJar") {
    archiveFileName.set("app.jar")
    mainClassName = "com.example.demo.Demo"
}

tasks.named<BootRun>("bootRun") {
    mainClass.set("com.example.demo.Demo")
    args("--spring.profiles.active=demo")
}
// end::lazy[]
