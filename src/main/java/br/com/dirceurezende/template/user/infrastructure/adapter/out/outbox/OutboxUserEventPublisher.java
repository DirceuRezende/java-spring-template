package br.com.dirceurezende.template.user.infrastructure.adapter.out.outbox;

import br.com.dirceurezende.template.user.application.port.out.UserEventPublisher;
import br.com.dirceurezende.template.user.domain.WelcomeRequested;
import br.com.dirceurezende.template.user.infrastructure.config.MessagingConfig;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

/** Implementa a porta gravando no outbox; quem fala com o Kafka é o OutboxRelay. */
@Component
class OutboxUserEventPublisher implements UserEventPublisher {

    /** Contrato da mensagem no tópico (JSON). */
    record WelcomeRequestedMessage(String userId, String email, String name, Instant occurredAt) {
    }

    private final SpringDataOutboxRepository outbox;
    private final JsonMapper json;

    OutboxUserEventPublisher(SpringDataOutboxRepository outbox, JsonMapper json) {
        this.outbox = outbox;
        this.json = json;
    }

    @Override
    public void publish(WelcomeRequested event) {
        var message = new WelcomeRequestedMessage(event.userId().toString(), event.email().value(),
                event.name(), event.occurredAt());
        // chave = userId: mensagens do mesmo usuário caem na mesma partição, em ordem
        outbox.save(new OutboxJpaEntity(MessagingConfig.WELCOME_REQUESTED_TOPIC, message.userId(),
                json.writeValueAsString(message), event.occurredAt()));
    }
}
