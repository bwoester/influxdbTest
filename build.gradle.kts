plugins {
    `java-library`
}

group = "de.benjamin-woester"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.7.2"))
    testImplementation("org.junit.jupiter", "junit-jupiter")
    testImplementation("org.junit.jupiter", "junit-jupiter-params")
    testImplementation("com.influxdb", "influxdb-client-java", "2.0.0")
}

tasks {
    test {
        useJUnitPlatform()
    }
}