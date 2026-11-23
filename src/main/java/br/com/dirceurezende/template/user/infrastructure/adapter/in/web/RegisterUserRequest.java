package br.com.dirceurezende.template.user.infrastructure.adapter.in.web;

import br.com.dirceurezende.template.user.application.port.in.RegisterUserCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO HTTP: validação de formato na borda; invariantes de verdade ficam no domínio. */
public record RegisterUserRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password) {

    RegisterUserCommand toCommand() {
        return new RegisterUserCommand(name, email, password);
    }

    @Override
    public String toString() {
        return "RegisterUserRequest[name=" + name + ", email=" + email + ", password=***]";
    }
}
