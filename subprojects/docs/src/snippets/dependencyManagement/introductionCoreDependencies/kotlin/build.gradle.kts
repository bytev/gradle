repositories { // <1>
    google() // <2>
    mavenCentral()
}

dependencies { // <3>
    testImplementation("junit:junit:4.13.2") // <4>
    implementation("com.google.guava:guava:31.1-jre")
}
