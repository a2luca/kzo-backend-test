package de.firma.kuze;

import de.firma.kuze.mandant.MandantFehltException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class StartOhneMandantTest {

    @Test
    void startOhneProfilBrichtVerstaendlichAb(CapturedOutput ausgabe) {
        assertThatThrownBy(() -> new SpringApplicationBuilder(KundenzentrumApplication.class)
                .web(WebApplicationType.NONE)
                .run())
                .isInstanceOf(MandantFehltException.class);

        assertThat(ausgabe).contains("APPLICATION FAILED TO START")
                .contains("Kein gültiger Mandant gesetzt (kundenzentrum.mandant=null)")
                .contains("--spring.profiles.active=netze");
    }
}
