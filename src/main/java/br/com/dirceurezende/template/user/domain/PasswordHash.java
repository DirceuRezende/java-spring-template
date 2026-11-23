package br.com.dirceurezende.template.user.domain;

/**
 * Senha já transformada em hash. O domínio nunca vê a senha em texto puro;
 * quem gera o hash é a porta de saída {@code PasswordHasher}.
 */
public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("Hash de senha é obrigatório");
        }
    }

    @Override
    public String toString() {
        return "PasswordHash[***]";
    }
}
