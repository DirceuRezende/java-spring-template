package br.com.dirceurezende.template.user.application.port.in;

public record LoginCommand(String email, String rawPassword) {

    /** Nunca deixar a senha vazar em log. */
    @Override
    public String toString() {
        return "LoginCommand[email=" + email + ", rawPassword=***]";
    }
}
