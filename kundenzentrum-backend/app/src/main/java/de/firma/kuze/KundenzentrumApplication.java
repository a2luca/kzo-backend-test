package de.firma.kuze;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Scannt de.firma.kuze und damit Core und alle Adapter-Module. */
@SpringBootApplication
public class KundenzentrumApplication {

    public static void main(String[] args) {
        SpringApplication.run(KundenzentrumApplication.class, args);
    }
}
