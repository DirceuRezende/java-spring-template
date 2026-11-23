package br.com.dirceurezende.template.user.domain;

/** Violação de invariante do domínio (dado inválido que nunca deveria chegar aqui). */
public class DomainValidationException extends RuntimeException {
    public DomainValidationException(String message) {
        super(message);
    }
}
