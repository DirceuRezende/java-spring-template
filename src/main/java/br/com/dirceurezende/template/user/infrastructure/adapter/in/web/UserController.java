package br.com.dirceurezende.template.user.infrastructure.adapter.in.web;

import br.com.dirceurezende.template.user.application.port.in.DeleteUserUseCase;
import br.com.dirceurezende.template.user.application.port.in.GetUserUseCase;
import br.com.dirceurezende.template.user.application.port.in.ListUsersUseCase;
import br.com.dirceurezende.template.user.application.port.in.RegisterUserResult;
import br.com.dirceurezende.template.user.application.port.in.RegisterUserUseCase;
import br.com.dirceurezende.template.user.application.port.in.RequestWelcomeUseCase;
import br.com.dirceurezende.template.user.application.port.in.UpdateUserCommand;
import br.com.dirceurezende.template.user.application.port.in.UpdateUserUseCase;
import br.com.dirceurezende.template.user.domain.UserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/users")
class UserController {

    private final RegisterUserUseCase registerUser;
    private final GetUserUseCase getUser;
    private final ListUsersUseCase listUsers;
    private final UpdateUserUseCase updateUser;
    private final DeleteUserUseCase deleteUser;
    private final RequestWelcomeUseCase requestWelcome;

    UserController(RegisterUserUseCase registerUser, GetUserUseCase getUser, ListUsersUseCase listUsers,
                   UpdateUserUseCase updateUser, DeleteUserUseCase deleteUser, RequestWelcomeUseCase requestWelcome) {
        this.registerUser = registerUser;
        this.getUser = getUser;
        this.listUsers = listUsers;
        this.updateUser = updateUser;
        this.deleteUser = deleteUser;
        this.requestWelcome = requestWelcome;
    }

    @PostMapping
    ResponseEntity<?> register(@Valid @RequestBody RegisterUserRequest request) {
        return switch (registerUser.register(request.toCommand())) {
            case RegisterUserResult.Registered(var user) -> ResponseEntity
                    .created(ServletUriComponentsBuilder.fromCurrentRequest()
                            .path("/{id}").buildAndExpand(user.id()).toUri())
                    .body(UserResponse.from(user));
            case RegisterUserResult.EmailAlreadyTaken(var email) -> ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Email já cadastrado: " + email))
                    .build();
        };
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    PageResponse list(@RequestParam(defaultValue = "0") @Min(0) int page,
                      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(listUsers.list(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.name")
    ResponseEntity<UserResponse> byId(@PathVariable String id) {
        return getUser.byId(UserId.of(id))
                .map(UserResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.name")
    ResponseEntity<UserResponse> update(@PathVariable String id, @Valid @RequestBody UpdateUserRequest request) {
        return updateUser.update(new UpdateUserCommand(UserId.of(id), request.name()))
                .map(UserResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.name")
    ResponseEntity<Void> delete(@PathVariable String id) {
        return deleteUser.delete(UserId.of(id))
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    /** Assíncrono: grava no outbox e responde 202; o envio acontece via Kafka. */
    @PostMapping("/{id}/welcome")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.name")
    ResponseEntity<Void> welcome(@PathVariable String id) {
        return requestWelcome.request(UserId.of(id))
                ? ResponseEntity.accepted().build()
                : ResponseEntity.notFound().build();
    }
}
