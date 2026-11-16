plugins {
    id("kuze.sap-client")
    `java-test-fixtures`
}

sapClient {
    teilSpec = "common.yaml"
    paket = "de.firma.kuze.sap.common.client"
}

dependencies {
    api(project(":core"))
    // SapProperties, RestClient.Builder und @ConfigurationProperties
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
