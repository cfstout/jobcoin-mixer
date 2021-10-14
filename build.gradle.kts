plugins {
    java
    application
}

repositories {
    mavenCentral()
}

tasks {
    test {
        useJUnitPlatform()
    }
}

dependencies {
    annotationProcessor( "org.projectlombok", "lombok", "1.18.22")
    compileOnly("org.projectlombok", "lombok", "1.18.22")

    implementation("io.dropwizard", "dropwizard-core","2.0.25")
    implementation("org.asynchttpclient","async-http-client","2.12.3")
    implementation("ru.vyarus", "dropwizard-guicey", "5.3.0")

    testImplementation("org.assertj", "assertj-core", "3.21.0")
    testImplementation("org.junit.jupiter", "junit-jupiter", "5.8.1")
}

