plugins {
    id("kuze.java-conventions")
}

// Der Core hängt von keinem anderen Modul dieses Projekts ab, nur von Spring.
dependencies {
    api("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
