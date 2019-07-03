subprojects {
    apply(plugin = "groovy")
    repositories {
        jcenter()
    }
    dependencies {
        "implementation"(localGroovy())
    }
// tag::enable-groovy-incremental[]
    tasks.withType<GroovyCompile> {
        options.isIncremental = true
    }
// end::enable-groovy-incremental[]
}

project(":app") {
    dependencies {
        implementation(project(":library"))
    }
}
