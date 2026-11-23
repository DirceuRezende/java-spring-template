package br.com.dirceurezende.template.user.infrastructure.adapter.in.web;

import org.junit.jupiter.api.Test;
import br.com.dirceurezende.template.PostgresIntegrationTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** CRUD via HTTP -> use case -> Postgres. Autorização fina fica em SecurityTest; aqui o chamador é ADMIN. */
@WithMockUser(roles = "ADMIN")
class UserControllerTest extends PostgresIntegrationTest {

    private static String body(String name, String email, String password) {
        return "{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}".formatted(name, email, password);
    }

    private ResultActions create(String json) throws Exception {
        return mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String register(String email) throws Exception {
        var location = create(body("Ana", email, "secret123")).andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private ResultActions rename(String id, String name) throws Exception {
        return mvc.perform(put("/api/users/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\"}".formatted(name)));
    }

    // ---- POST
    @Test
    void postCreatesWithLocationAndNoPassword() throws Exception {
        create(body("Ana", "ana@example.com", "secret123"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/users/")))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(content().string(not(containsString("secret123"))))
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void postDuplicateEmailIsConflictCaseInsensitive() throws Exception {
        register("dup@example.com");
        create(body("Outra", "DUP@example.com", "secret123")).andExpect(status().isConflict());
    }

    @Test
    void postInvalidPayloadIs400() throws Exception {
        create(body("Ana", "a@example.com", "123")).andExpect(status().isBadRequest());
        create(body("Ana", "not-an-email", "secret123")).andExpect(status().isBadRequest());
        create(body(" ", "a@example.com", "secret123")).andExpect(status().isBadRequest());
    }

    // ---- GET /{id}
    @Test
    void getByIdFoundNotFoundAndMalformed() throws Exception {
        var id = register("get@example.com");
        mvc.perform(get("/api/users/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/users/" + UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/api/users/not-a-uuid")).andExpect(status().isBadRequest());
    }

    // ---- GET list
    @Test
    void listIsPaginated() throws Exception {
        register("l1@example.com");
        register("l2@example.com");
        register("l3@example.com");

        mvc.perform(get("/api/users?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void listUsesDefaults() throws Exception {
        mvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void listRejectsInvalidPaging() throws Exception {
        mvc.perform(get("/api/users?page=-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users?size=0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users?size=101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users?page=abc")).andExpect(status().isBadRequest());
    }

    // ---- PUT
    @Test
    void putRenamesTrimmed() throws Exception {
        var id = register("put@example.com");
        rename(id, "  Bia ").andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Bia"));
        mvc.perform(get("/api/users/" + id)).andExpect(jsonPath("$.name").value("Bia"));
    }

    @Test
    void putNotFoundAndBlankName() throws Exception {
        rename(UUID.randomUUID().toString(), "Bia").andExpect(status().isNotFound());
        var id = register("put2@example.com");
        rename(id, " ").andExpect(status().isBadRequest());
    }

    // ---- DELETE
    @Test
    void deleteIsSoftAndIdempotent() throws Exception {
        var id = register("del@example.com");
        mvc.perform(delete("/api/users/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/users/" + id)).andExpect(jsonPath("$.active").value(false));
        mvc.perform(delete("/api/users/" + id)).andExpect(status().isNoContent());
    }

    @Test
    void deleteNotFound() throws Exception {
        mvc.perform(delete("/api/users/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }
}
