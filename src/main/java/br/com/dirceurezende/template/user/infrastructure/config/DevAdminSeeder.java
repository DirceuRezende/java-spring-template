package br.com.dirceurezende.template.user.infrastructure.config;

import br.com.dirceurezende.template.user.application.port.out.PasswordHasher;
import br.com.dirceurezende.template.user.application.port.out.UserRepository;
import br.com.dirceurezende.template.user.domain.Email;
import br.com.dirceurezende.template.user.domain.Role;
import br.com.dirceurezende.template.user.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

/** Só existe no profile "dev": em produção o primeiro admin é provisionado por fora. */
@Configuration(proxyBeanMethods = false)
@Profile("dev")
class DevAdminSeeder {

    @Bean
    ApplicationRunner seedAdmin(UserRepository users, PasswordHasher hasher, Clock clock,
                                @Value("${app.dev-admin.email}") String email,
                                @Value("${app.dev-admin.password}") String password) {
        return args -> {
            var adminEmail = new Email(email);
            if (!users.existsByEmail(adminEmail)) {
                var admin = User.register(adminEmail, "Admin", hasher.hash(password), clock.instant());
                admin.grant(Role.ADMIN);
                users.save(admin);
            }
        };
    }
}
