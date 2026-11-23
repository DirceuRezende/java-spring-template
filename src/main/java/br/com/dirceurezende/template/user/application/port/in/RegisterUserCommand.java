package br.com.dirceurezende.template.user.application.port.in;

public record RegisterUserCommand(String name, String email, String rawPassword) {

    /** Nunca deixar a senha vazar em log. */
    @Override
    public String toString() {
        return "RegisterUserCommand[name=" + name + ", email=" + email + ", rawPassword=***]";
    }
}
