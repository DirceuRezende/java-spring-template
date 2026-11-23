package br.com.dirceurezende.template.user.infrastructure.config;

import io.micrometer.observation.ObservationPredicate;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.support.ScheduledTaskObservationContext;

/** Traces e métricas são auto-configurados pelo Boot; aqui só os ajustes do projeto. */
@Configuration(proxyBeanMethods = false)
class ObservabilityConfig {

    /** O appender do logback-spring.xml precisa receber o SDK para enviar os logs via OTLP. */
    @Bean
    InitializingBean installOpenTelemetryLogAppender(OpenTelemetry openTelemetry) {
        return () -> OpenTelemetryAppender.install(openTelemetry);
    }

    /**
     * O OutboxRelay faz polling a cada 1s: sem isto, cada ciclo vira um trace vazio no Tempo.
     * Os envios continuam rastreados (span "outbox relay" no trace da requisição de origem).
     */
    @Bean
    ObservationPredicate skipOutboxPollingObservation() {
        return (name, context) -> !(context instanceof ScheduledTaskObservationContext task
                && task.getTargetClass().getSimpleName().equals("OutboxRelay"));
    }
}
