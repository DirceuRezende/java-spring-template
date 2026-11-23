package br.com.dirceurezende.template.user.application.port.out;

public record AccessToken(String value, long expiresInSeconds) {}
