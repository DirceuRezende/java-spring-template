package br.com.dirceurezende.template.user.infrastructure.adapter.out.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Mensagem pendente de publicação. Gravada na mesma transação da mudança de negócio. */
@Entity
@Table(name = "outbox")
class OutboxJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 255)
    private String topic;

    @Column(name = "message_key", nullable = false, length = 255)
    private String messageKey;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    /** W3C traceparent de quem gerou a mensagem; nulo quando não havia trace ativo. */
    @Column(name = "trace_parent", length = 55)
    private String traceParent;

    protected OutboxJpaEntity() {
        // exigido pelo JPA
    }

    OutboxJpaEntity(String topic, String messageKey, String payload, Instant createdAt, String traceParent) {
        this.id = UUID.randomUUID();
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.createdAt = createdAt;
        this.traceParent = traceParent;
    }

    void markPublished(Instant at) { this.publishedAt = at; }

    String getTopic() { return topic; }
    String getMessageKey() { return messageKey; }
    String getPayload() { return payload; }
    Instant getPublishedAt() { return publishedAt; }
    String getTraceParent() { return traceParent; }
}
