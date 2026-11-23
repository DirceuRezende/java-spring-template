package br.com.dirceurezende.template;

import com.jayway.jsonpath.JsonPath;
import br.com.dirceurezende.template.user.application.port.out.PasswordHasher;
import br.com.dirceurezende.template.user.application.port.out.UserRepository;
import br.com.dirceurezende.template.user.domain.Email;
import br.com.dirceurezende.template.user.domain.Role;
import br.com.dirceurezende.template.user.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Base dos testes de integração: app inteira + Postgres real (Testcontainers) + rollback por teste. */
@SpringBootTest(properties = "app.security.jwt-secret=test-secret-0123456789abcdef0123456789")
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
public abstract class PostgresIntegrationTest {

    public static final String PASSWORD = "secret12345";

    public record Session(String id, String email, String accessToken, String refreshToken) {
        public String bearer() {
            return "Bearer " + accessToken;
        }
    }

    @Autowired protected MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordHasher hasher;

    /** Cadastro público + login. */
    protected Session signup(String email) throws Exception {
        var location = mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        return login(email, location.substring(location.lastIndexOf('/') + 1));
    }

    /** ADMIN criado direto no repositório (não existe rota para isso). */
    protected Session admin(String email) throws Exception {
        var admin = User.register(new Email(email), "Admin", hasher.hash(PASSWORD), Instant.now());
        admin.grant(Role.ADMIN);
        users.save(admin);
        return login(email, admin.id().toString());
    }

    protected Session login(String email, String id) throws Exception {
        var body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new Session(id, email, JsonPath.read(body, "$.accessToken"), JsonPath.read(body, "$.refreshToken"));
    }
}
