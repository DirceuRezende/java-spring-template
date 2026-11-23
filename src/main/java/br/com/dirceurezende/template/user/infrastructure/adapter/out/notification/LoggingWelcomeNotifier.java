package br.com.dirceurezende.template.user.infrastructure.adapter.out.notification;

import br.com.dirceurezende.template.user.application.port.out.WelcomeNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Stand-in de um envio real (e-mail, push...): troque este adapter sem tocar no core. */
@Component
class LoggingWelcomeNotifier implements WelcomeNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingWelcomeNotifier.class);

    @Override
    public void welcome(String email, String name) {
        log.info("Boas-vindas enviadas para {} <{}>", name, email);
    }
}
