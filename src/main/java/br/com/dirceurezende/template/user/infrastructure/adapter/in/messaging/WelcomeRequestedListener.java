package br.com.dirceurezende.template.user.infrastructure.adapter.in.messaging;

import br.com.dirceurezende.template.user.application.port.in.SendWelcomeUseCase;
import br.com.dirceurezende.template.user.domain.Email;
import br.com.dirceurezende.template.user.domain.UserId;
import br.com.dirceurezende.template.user.domain.WelcomeRequested;
import br.com.dirceurezende.template.user.infrastructure.config.MessagingConfig;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

/** Adapter de entrada: Kafka → caso de uso. Entrega é at-least-once, então o caso de uso deve tolerar repetição. */
@Component
class WelcomeRequestedListener {

    /** Contrato da mensagem no tópico, do lado do consumidor. */
    record WelcomeRequestedMessage(String userId, String email, String name, Instant occurredAt) {
    }

    private final SendWelcomeUseCase sendWelcome;
    private final JsonMapper json;

    WelcomeRequestedListener(SendWelcomeUseCase sendWelcome, JsonMapper json) {
        this.sendWelcome = sendWelcome;
        this.json = json;
    }

    @KafkaListener(topics = MessagingConfig.WELCOME_REQUESTED_TOPIC)
    void on(String payload) {
        var message = json.readValue(payload, WelcomeRequestedMessage.class);
        sendWelcome.send(new WelcomeRequested(UserId.of(message.userId()), new Email(message.email()),
                message.name(), message.occurredAt()));
    }
}
