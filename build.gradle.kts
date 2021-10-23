plugins {
    java
    application
}

application {
    mainClass.set("io.github.cfstout.jobcoin.JobCoinMixer")
    applicationDefaultJvmArgs.let { existing -> existing + listOf("server", "config.yaml")}
}

repositories {
    mavenCentral()
}

tasks {
    test {
        useJUnitPlatform()
    }
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "io.github.cfstout.jobcoin.JobCoinMixer"
    }
}

val deps by extra {
    mapOf(
        "asynchttp" to "2.12.3",
        "assertj" to "3.21.0",
        "dropwizard" to "2.0.25",
        "guicey" to "5.3.0",
        "junit" to "5.8.1",
        "lombok" to "1.18.22",
        "mockito" to "4.0.0"
    )
}

dependencies {
    annotationProcessor("org.projectlombok", "lombok", deps["lombok"])
    compileOnly("org.projectlombok", "lombok", deps["lombok"])

    implementation("io.dropwizard", "dropwizard-core", deps["dropwizard"])
    implementation("org.asynchttpclient", "async-http-client", deps["asynchttp"])
    implementation("ru.vyarus", "dropwizard-guicey", deps["guicey"])

    testImplementation("org.assertj", "assertj-core", deps["assertj"])
    testImplementation("org.junit.jupiter", "junit-jupiter", deps["junit"])
    testImplementation("org.junit.jupiter", "junit-jupiter-params", deps["junit"])
    implementation("org.mockito", "mockito-core", deps["mockito"])
}

