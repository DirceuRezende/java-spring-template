package br.com.dirceurezende.template.user.infrastructure.adapter.in.web;

import br.com.dirceurezende.template.user.application.port.in.LoginCommand;
import br.com.dirceurezende.template.user.application.port.in.LoginUseCase;
import br.com.dirceurezende.template.user.application.port.in.LogoutUseCase;
import br.com.dirceurezende.template.user.application.port.in.RefreshTokenUseCase;
import br.com.dirceurezende.template.user.application.port.in.TokenPair;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    record LoginRequest(@NotBlank String email, @NotBlank String password) {
        @Override
        public String toString() {
            return "LoginRequest[email=" + email + ", password=***]";
        }
    }

    record RefreshRequest(@NotBlank String refreshToken) {
        @Override
        public String toString() {
            return "RefreshRequest[refreshToken=***]";
        }
    }

    record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
        static TokenResponse from(TokenPair pair) {
            return new TokenResponse(pair.accessToken(), pair.refreshToken(), "Bearer", pair.expiresInSeconds());
        }
    }

    private final LoginUseCase login;
    private final RefreshTokenUseCase refresh;
    private final LogoutUseCase logout;

    AuthController(LoginUseCase login, RefreshTokenUseCase refresh, LogoutUseCase logout) {
        this.login = login;
        this.refresh = refresh;
        this.logout = logout;
    }

    @PostMapping("/login")
    ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return respond(login.login(new LoginCommand(request.email(), request.password())), "Credenciais inválidas");
    }

    @PostMapping("/refresh")
    ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
        return respond(refresh.refresh(request.refreshToken()), "Refresh token inválido ou expirado");
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        logout.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<?> respond(Optional<TokenPair> pair, String failure) {
        return pair.<ResponseEntity<?>>map(p -> ResponseEntity.ok(TokenResponse.from(p)))
                .orElseGet(() -> ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, failure)).build());
    }
}
