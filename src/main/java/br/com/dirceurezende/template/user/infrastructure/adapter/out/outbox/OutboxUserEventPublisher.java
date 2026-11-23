package br.com.dirceurezende.template.user.infrastructure.adapter.out.outbox;

import br.com.dirceurezende.template.user.application.port.out.UserEventPublisher;
import br.com.dirceurezende.template.user.domain.WelcomeRequested;
import br.com.dirceurezende.template.user.infrastructure.config.MessagingConfig;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.HashMap;

/** Implementa a porta gravando no outbox; quem fala com o Kafka é o OutboxRelay. */
@Component
class OutboxUserEventPublisher implements UserEventPublisher {

    /** Contrato da mensagem no tópico (JSON). */
    record WelcomeRequestedMessage(String userId, String email, String name, Instant occurredAt) {
    }

    private final SpringDataOutboxRepository outbox;
    private final JsonMapper json;
    private final Tracer tracer;
    private final Propagator propagator;

    OutboxUserEventPublisher(SpringDataOutboxRepository outbox, JsonMapper json, Tracer tracer, Propagator propagator) {
        this.outbox = outbox;
        this.json = json;
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Override
    public void publish(WelcomeRequested event) {
        var message = new WelcomeRequestedMessage(event.userId().toString(), event.email().value(),
                event.name(), event.occurredAt());
        // chave = userId: mensagens do mesmo usuário caem na mesma partição, em ordem
        outbox.save(new OutboxJpaEntity(MessagingConfig.WELCOME_REQUESTED_TOPIC, message.userId(),
                json.writeValueAsString(message), event.occurredAt(), currentTraceParent()));
    }

    private String currentTraceParent() {
        var context = tracer.currentTraceContext().context();
        if (context == null) return null;
        var headers = new HashMap<String, String>();
        propagator.inject(context, headers, HashMap::put);
        return headers.get("traceparent");
    }
}
