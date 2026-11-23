package br.com.dirceurezende.template.user.infrastructure.adapter.in.web;

import br.com.dirceurezende.template.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityTest extends PostgresIntegrationTest {

    @Autowired JwtEncoder encoder;

    private ResultActions json(String method, String url, String token, String body) throws Exception {
        var req = request(org.springframework.http.HttpMethod.valueOf(method), url)
                .contentType(MediaType.APPLICATION_JSON);
        if (body != null) req.content(body);
        if (token != null) req.header("Authorization", token);
        return mvc.perform(req);
    }

    private ResultActions refreshWith(String refreshToken) throws Exception {
        return json("POST", "/api/auth/refresh", null, "{\"refreshToken\":\"%s\"}".formatted(refreshToken));
    }

    // ---- autenticação
    @Test
    void protectedRoutesNeedAToken() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/users/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedAndExpiredTokensAreRejected() throws Exception {
        var ana = signup("ana@example.com");
        var tampered = ana.accessToken().substring(0, ana.accessToken().length() - 2) + "xx";
        mvc.perform(get("/api/users/" + ana.id()).header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());

        var expired = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().subject(ana.id()).claim("roles", List.of("ADMIN"))
                        .issuedAt(Instant.now().minusSeconds(7200)).expiresAt(Instant.now().minusSeconds(3600)).build()));
        mvc.perform(get("/api/users/" + ana.id()).header("Authorization", "Bearer " + expired.getTokenValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRejectsWrongPasswordUnknownUserAndBlankBody() throws Exception {
        signup("ana@example.com");
        json("POST", "/api/auth/login", null, "{\"email\":\"ana@example.com\",\"password\":\"wrong-password\"}")
                .andExpect(status().isUnauthorized());
        json("POST", "/api/auth/login", null, "{\"email\":\"nobody@example.com\",\"password\":\"secret12345\"}")
                .andExpect(status().isUnauthorized());
        json("POST", "/api/auth/login", null, "{\"email\":\"\",\"password\":\"\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousSignupIsAllowedAndAlwaysCreatesPlainUser() throws Exception {
        var ana = signup("ana@example.com");
        mvc.perform(get("/api/users/" + ana.id()).header("Authorization", ana.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.roles.length()").value(1));
    }

    // ---- autorização
    @Test
    void userManagesOnlyThemselves() throws Exception {
        var ana = signup("ana@example.com");
        var bob = signup("bob@example.com");

        mvc.perform(get("/api/users/" + ana.id()).header("Authorization", ana.bearer())).andExpect(status().isOk());
        json("PUT", "/api/users/" + ana.id(), ana.bearer(), "{\"name\":\"Ana 2\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Ana 2"));

        mvc.perform(get("/api/users/" + bob.id()).header("Authorization", ana.bearer())).andExpect(status().isForbidden());
        json("PUT", "/api/users/" + bob.id(), ana.bearer(), "{\"name\":\"x\"}").andExpect(status().isForbidden());
        mvc.perform(delete("/api/users/" + bob.id()).header("Authorization", ana.bearer())).andExpect(status().isForbidden());

        mvc.perform(delete("/api/users/" + ana.id()).header("Authorization", ana.bearer())).andExpect(status().isNoContent());
    }

    @Test
    void onlyAdminCanList() throws Exception {
        var ana = signup("ana@example.com");
        var admin = admin("admin@example.com");

        mvc.perform(get("/api/users").header("Authorization", ana.bearer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/users").header("Authorization", admin.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void adminCanManageAnyUser() throws Exception {
        var ana = signup("ana@example.com");
        var admin = admin("admin@example.com");

        mvc.perform(get("/api/users/" + ana.id()).header("Authorization", admin.bearer())).andExpect(status().isOk());
        json("PUT", "/api/users/" + ana.id(), admin.bearer(), "{\"name\":\"Renomeada\"}").andExpect(status().isOk());
        mvc.perform(delete("/api/users/" + ana.id()).header("Authorization", admin.bearer())).andExpect(status().isNoContent());
    }

    @Test
    void welcomeIsForSelfOrAdmin() throws Exception {
        var ana = signup("ana@example.com");
        var bob = signup("bob@example.com");
        var admin = admin("admin@example.com");

        mvc.perform(post("/api/users/" + ana.id() + "/welcome")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/users/" + bob.id() + "/welcome").header("Authorization", ana.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/users/" + ana.id() + "/welcome").header("Authorization", ana.bearer()))
                .andExpect(status().isAccepted());
        mvc.perform(post("/api/users/" + bob.id() + "/welcome").header("Authorization", admin.bearer()))
                .andExpect(status().isAccepted());
        mvc.perform(post("/api/users/" + UUID.randomUUID() + "/welcome").header("Authorization", admin.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void openApiAndSwaggerArePublic() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/users/{id}/welcome']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearer.scheme").value("bearer"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    // ---- refresh / logout
    @Test
    void refreshRotatesToken() throws Exception {
        var ana = signup("ana@example.com");

        refreshWith(ana.refreshToken()).andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
        refreshWith(ana.refreshToken()).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        var ana = signup("ana@example.com");

        json("POST", "/api/auth/logout", null, "{\"refreshToken\":\"%s\"}".formatted(ana.refreshToken()))
                .andExpect(status().isNoContent());
        refreshWith(ana.refreshToken()).andExpect(status().isUnauthorized());
    }

    @Test
    void deletedUserCannotLoginOrRefresh() throws Exception {
        var ana = signup("ana@example.com");
        mvc.perform(delete("/api/users/" + ana.id()).header("Authorization", ana.bearer())).andExpect(status().isNoContent());

        refreshWith(ana.refreshToken()).andExpect(status().isUnauthorized());
        json("POST", "/api/auth/login", null, "{\"email\":\"ana@example.com\",\"password\":\"secret12345\"}")
                .andExpect(status().isUnauthorized());
    }
}
