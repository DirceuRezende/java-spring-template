package br.com.dirceurezende.template.user.infrastructure.adapter.out.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** jwtSecret não tem default: sem JWT_SECRET (ou propriedade) a aplicação não sobe. */
@ConfigurationProperties("app.security")
public record JwtProperties(String jwtSecret,
                            @DefaultValue("15m") Duration accessTtl,
                            @DefaultValue("7d") Duration refreshTtl) {

    public JwtProperties {
        // O binder deixa placeholder não resolvido como texto literal ("${JWT_SECRET}")
        if (jwtSecret == null || jwtSecret.startsWith("${")) {
            throw new IllegalStateException(
                    "app.security.jwt-secret não configurado: defina JWT_SECRET ou rode com o profile dev");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.security.jwt-secret deve ter ao menos 32 bytes");
        }
    }

    @Override
    public String toString() {
        return "JwtProperties[accessTtl=" + accessTtl + ", refreshTtl=" + refreshTtl + "]";
    }
}
