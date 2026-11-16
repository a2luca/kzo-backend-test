plugins {
    id("kuze.sap-client")
}

sapClient {
    teilSpec = "netze-rest.yaml"
    paket = "de.firma.kuze.sap.netze.client"
}

// Hängt von adapter-common ab (gemeinsamer RestClient, SapProperties), nie von adapter-odr.
dependencies {
    api(project(":core"))
    implementation(project(":adapter-common"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation(testFixtures(project(":adapter-common")))
}
