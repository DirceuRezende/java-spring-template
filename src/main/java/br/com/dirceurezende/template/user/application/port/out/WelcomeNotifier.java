package br.com.dirceurezende.template.user.application.port.out;

public interface WelcomeNotifier {

    void welcome(String email, String name);
}
