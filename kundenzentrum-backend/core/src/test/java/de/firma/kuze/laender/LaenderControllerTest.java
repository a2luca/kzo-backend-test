package de.firma.kuze.laender;

import de.firma.kuze.fehler.FehlerHandler;
import de.firma.kuze.fehler.NichtGefundenException;
import de.firma.kuze.fehler.SapNichtVerfuegbarException;
import org.junit.jupiter.api.Test;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LaenderControllerTest {

    private static MockMvc mockMvc(LaenderPort port) {
        return MockMvcBuilders.standaloneSetup(new LaenderController(new LaenderService(port)))
                .setControllerAdvice(new FehlerHandler())
                .build();
    }

    @Test
    void liefertLaenderImFrontendFormat() throws Exception {
        mockMvc(() -> List.of(new LandDto("DE", "Deutschland")))
                .perform(get("/api/laender"))
                .andExpect(status().isOk())
                .andExpect(content().json("[{\"code\":\"DE\",\"name\":\"Deutschland\"}]", JsonCompareMode.STRICT));
    }

    @Test
    void nichtGefundenWird404() throws Exception {
        mockMvc(() -> {
            throw new NichtGefundenException("weg", null);
        })
                .perform(get("/api/laender"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("{\"fehler\":\"NICHT_GEFUNDEN\"}", JsonCompareMode.STRICT));
    }

    @Test
    void sapNichtVerfuegbarWird502() throws Exception {
        mockMvc(() -> {
            throw new SapNichtVerfuegbarException("down", null);
        })
                .perform(get("/api/laender"))
                .andExpect(status().isBadGateway())
                .andExpect(content().json("{\"fehler\":\"SAP_NICHT_VERFUEGBAR\"}", JsonCompareMode.STRICT));
    }
}
