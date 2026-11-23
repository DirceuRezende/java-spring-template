package br.com.dirceurezende.template;

import br.com.dirceurezende.template.user.application.port.out.WelcomeNotifier;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caminho completo: HTTP → outbox (Postgres) → relay → Kafka → listener → WelcomeNotifier.
 * Sem @Transactional de propósito: o outbox só é lido pelo relay depois do commit.
 */
@SpringBootTest(properties = {
        "app.security.jwt-secret=test-secret-0123456789abcdef0123456789",
        "app.outbox.poll-interval=200ms"})
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class WelcomeFlowTest {

    @Autowired MockMvc mvc;
    @MockitoSpyBean WelcomeNotifier notifier;

    @Test
    void welcomeTravelsThroughOutboxAndKafka() throws Exception {
        var email = "flow-" + UUID.randomUUID() + "@example.com";
        var location = mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"%s\",\"password\":\"secret12345\"}".formatted(email)))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        var id = location.substring(location.lastIndexOf('/') + 1);
        var token = JsonPath.<String>read(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"secret12345\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.accessToken");

        mvc.perform(post("/api/users/" + id + "/welcome").header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted());

        verify(notifier, timeout(30_000)).welcome(email, "Ana");
    }
}
