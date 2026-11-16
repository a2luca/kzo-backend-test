plugins {
    id("kuze.sap-client")
}

sapClient {
    teilSpec = "odr-rest.yaml"
    paket = "de.firma.kuze.sap.odr.client"
}

// Hängt von adapter-common ab (gemeinsamer RestClient, SapProperties), nie von adapter-netze.
dependencies {
    api(project(":core"))
    implementation(project(":adapter-common"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation(testFixtures(project(":adapter-common")))
}
