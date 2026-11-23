package br.com.dirceurezende.template;

import br.com.dirceurezende.template.user.application.port.out.WelcomeNotifier;
import com.jayway.jsonpath.JsonPath;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caminho completo: HTTP → outbox (Postgres) → relay → Kafka → listener → WelcomeNotifier, num trace só.
 * Sem @Transactional de propósito: o outbox só é lido pelo relay depois do commit.
 */
@SpringBootTest(properties = {
        "app.security.jwt-secret=test-secret-0123456789abcdef0123456789",
        "app.outbox.poll-interval=200ms"})
@AutoConfigureMockMvc
@AutoConfigureTracing // tracer real (nos outros testes é noop); exportadores OTLP desligados em test/resources/config
@Import(TestcontainersConfig.class)
class WelcomeFlowTest {

    @Autowired MockMvc mvc;
    @MockitoSpyBean WelcomeNotifier notifier;
    @Autowired Tracer tracer;

    @Test
    void welcomeTravelsThroughOutboxAndKafkaInTheSameTrace() throws Exception {
        var traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        var traceIdSeenByConsumer = new AtomicReference<String>();
        doAnswer(call -> {
            traceIdSeenByConsumer.set(tracer.currentSpan().context().traceId());
            return call.callRealMethod();
        }).when(notifier).welcome(anyString(), anyString());

        var email = "flow-" + UUID.randomUUID() + "@example.com";
        var location = mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"%s\",\"password\":\"secret12345\"}".formatted(email)))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        var id = location.substring(location.lastIndexOf('/') + 1);
        var token = JsonPath.<String>read(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"secret12345\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.accessToken");

        mvc.perform(post("/api/users/" + id + "/welcome").header("Authorization", "Bearer " + token)
                        .header("traceparent", "00-" + traceId + "-00f067aa0ba902b7-01"))
                .andExpect(status().isAccepted());

        verify(notifier, timeout(30_000)).welcome(email, "Ana");
        assertThat(traceIdSeenByConsumer.get()).isEqualTo(traceId);
    }
}
