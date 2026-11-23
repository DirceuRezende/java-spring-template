package br.com.dirceurezende.template.user.application.port.in;

/**
 * Resultados possíveis do caso de uso. Sealed: o compilador obriga
 * o adapter de entrada a tratar todos os casos no switch.
 */
public sealed interface RegisterUserResult {

    record Registered(UserView user) implements RegisterUserResult {}

    record EmailAlreadyTaken(String email) implements RegisterUserResult {}
}
