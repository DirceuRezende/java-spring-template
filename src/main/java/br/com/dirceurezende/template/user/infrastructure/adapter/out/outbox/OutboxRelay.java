package br.com.dirceurezende.template.user.infrastructure.adapter.out.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Lê o outbox e publica no Kafka. Só marca como publicado depois do ack do broker;
 * se falhar no meio do lote, tudo volta e é reenviado no próximo ciclo (at-least-once).
 */
@Component
class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 100;

    private final SpringDataOutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate tx;
    private final Clock clock;

    OutboxRelay(SpringDataOutboxRepository outbox, KafkaTemplate<String, String> kafka,
                PlatformTransactionManager txManager, Clock clock) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.tx = new TransactionTemplate(txManager);
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval:1s}")
    void relay() {
        try {
            tx.executeWithoutResult(_ -> outbox.lockPending(BATCH_SIZE).forEach(message -> {
                send(message);
                message.markPublished(clock.instant());
            }));
        } catch (RuntimeException e) {
            log.warn("Outbox: falha ao publicar no Kafka, nova tentativa no próximo ciclo: {}", e.getMessage());
        }
    }

    private void send(OutboxJpaEntity message) {
        try {
            kafka.send(message.getTopic(), message.getMessageKey(), message.getPayload()).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrompido publicando no Kafka", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Kafka não confirmou o envio", e);
        }
    }
}
