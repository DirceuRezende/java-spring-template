package br.com.dirceurezende.template.user.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Tópicos do Kafka (criados pelo KafkaAdmin no startup) + agendamento do relay do outbox. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class MessagingConfig {

    public static final String WELCOME_REQUESTED_TOPIC = "user.welcome-requested";

    @Bean
    NewTopic welcomeRequestedTopic() {
        return TopicBuilder.name(WELCOME_REQUESTED_TOPIC).partitions(3).replicas(1).build();
    }
}
