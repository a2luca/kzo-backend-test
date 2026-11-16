plugins {
    id("kuze.java-conventions")
    application
}

// Steht bewusst neben der Anwendung: hängt von keinem anderen Modul ab und wird nicht ausgeliefert.
dependencies {
    implementation(libs.wiremock)
}

application {
    mainClass = "de.firma.kuze.sapmock.SapMock"
}
